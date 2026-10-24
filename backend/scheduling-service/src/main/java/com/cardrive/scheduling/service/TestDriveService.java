package com.cardrive.scheduling.service;

import com.cardrive.scheduling.aop.Audited;
import com.cardrive.scheduling.client.CarLookupService;
import com.cardrive.scheduling.client.dto.CarDTO;
import com.cardrive.scheduling.domain.entity.Availability;
import com.cardrive.scheduling.domain.entity.CarScheduleConfig;
import com.cardrive.scheduling.domain.entity.TestDrive;
import com.cardrive.scheduling.domain.entity.TestDriveStatus;
import com.cardrive.scheduling.dto.CreateTestDriveRequest;
import com.cardrive.scheduling.dto.RescheduleRequest;
import com.cardrive.scheduling.dto.TestDriveResponse;
import com.cardrive.scheduling.exception.ConflictException;
import com.cardrive.scheduling.exception.ForbiddenException;
import com.cardrive.scheduling.exception.InvalidStateTransitionException;
import com.cardrive.scheduling.exception.NotFoundException;
import com.cardrive.scheduling.exception.ValidationException;
import com.cardrive.scheduling.outbox.OutboxEventFactory;
import com.cardrive.scheduling.repository.AvailabilityRepository;
import com.cardrive.scheduling.repository.CarScheduleConfigRepository;
import com.cardrive.scheduling.repository.OutboxEventRepository;
import com.cardrive.scheduling.repository.TestDriveRepository;
import com.cardrive.scheduling.schedulingkafka.event.TestDriveCompletedEvent;
import com.cardrive.scheduling.schedulingkafka.event.TestDriveScheduledEvent;
import com.cardrive.scheduling.schedulingkafka.kafka.TestDriveEventProducer;
import com.cardrive.scheduling.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core scheduling logic.
 *
 * Identity is username-keyed throughout (see security.CurrentUser javadoc) -
 * matches auth-service's User entity, which is keyed by username with no
 * numeric user id anywhere in the platform.
 *
 * Layered conflict protection (both layers are required, neither is sufficient alone):
 *   1. Application pre-check (findOverlapping) - fails fast with a clean 409 in the
 *      common case, before we even attempt a write.
 *   2. PostgreSQL partial EXCLUDE USING gist constraint (V2 migration) - the actual
 *      source of truth. If two concurrent transactions both pass step 1 (the classic
 *      check-then-act race), the second INSERT/UPDATE to commit is rejected by Postgres
 *      with a DataIntegrityViolationException, which we translate to 409 in
 *      GlobalExceptionHandler. This works correctly across multiple service instances
 *      because the guarantee lives in the shared database, not in JVM memory.
 *
 * Status-transition endpoints (confirm/reject/cancel/reschedule) additionally take a
 * PESSIMISTIC_WRITE row lock on the specific TestDrive id via findByIdForUpdate, so two
 * concurrent transitions on the *same* row (e.g. buyer cancels while seller confirms)
 * serialize instead of racing on a stale in-memory read.
 */
@Service
@RequiredArgsConstructor
public class TestDriveService {

    private final TestDriveRepository testDriveRepository;
    private final AvailabilityRepository availabilityRepository;
    private final CarScheduleConfigRepository carScheduleConfigRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventFactory outboxEventFactory;
    private final CarLookupService carLookupService;
    private final TestDriveEventProducer testDriveEventProducer;

    @Audited("TEST_DRIVE_CREATE")
    @Transactional
    public TestDriveResponse create(CurrentUser buyer, CreateTestDriveRequest request) {
        if (!buyer.isBuyer()) {
            throw new ForbiddenException("Only buyers can request test drives");
        }

        CarDTO car = carLookupService.getCarOrThrow(request.carId());

        int durationMinutes = resolveDuration(request.carId());
        LocalDateTime startTime = request.startTime();
        LocalDateTime endTime = startTime.plusMinutes(durationMinutes);

        if (!startTime.isAfter(LocalDateTime.now())) {
            throw new ValidationException("startTime must be in the future");
        }

        validateWithinAvailability(request.carId(), startTime, endTime);

        // Application-level fast-path check (see class javadoc - not sufficient alone).
        List<TestDrive> overlapping = testDriveRepository.findOverlapping(request.carId(), startTime, endTime);
        if (!overlapping.isEmpty()) {
            throw new ConflictException("Requested time overlaps an existing pending/confirmed test drive for this car");
        }

        TestDrive testDrive = TestDrive.builder()
                .carId(request.carId())
                .sellerUsername(car.sellerUsername())
                .buyerUsername(buyer.getUsername())
                .startTime(startTime)
                .endTime(endTime)
                .status(TestDriveStatus.PENDING)
                .build();

        // The INSERT below is where the database-level EXCLUDE constraint provides the
        // final, race-condition-proof guarantee, even if another instance's transaction
        // committed an overlapping row microseconds after our pre-check above ran.
        testDrive = testDriveRepository.save(testDrive);

        recordOutboxEvent(testDrive, "TEST_DRIVE_REQUESTED");

        testDriveEventProducer.publishTestDriveScheduled(
                new TestDriveScheduledEvent(
                        testDrive.getId(),
                        testDrive.getCarId(),
                        testDrive.getBuyerUsername(),
                        testDrive.getStartTime(),
                        testDrive.getStatus().toString()
                ));

        return TestDriveResponse.from(testDrive);
    }

    @Audited("TEST_DRIVE_CONFIRM")
    @Transactional
    public TestDriveResponse confirm(CurrentUser seller, Long testDriveId) {
        TestDrive testDrive = lockOrThrow(testDriveId);
        requireOwningSeller(seller, testDrive);

        if (testDrive.getStatus() != TestDriveStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Cannot confirm a test drive in status " + testDrive.getStatus() + "; only PENDING -> CONFIRMED is allowed");
        }

        testDrive.setStatus(TestDriveStatus.CONFIRMED);
        testDrive = testDriveRepository.save(testDrive);
        recordOutboxEvent(testDrive, "TEST_DRIVE_CONFIRMED");

        testDriveEventProducer.publishTestDriveCompleted(
                new TestDriveCompletedEvent(
                        testDrive.getId(),
                        testDrive.getCarId(),
                        testDrive.getBuyerUsername(),
                        testDrive.getEndTime()
                ));

        return TestDriveResponse.from(testDrive);
    }

    @Audited("TEST_DRIVE_REJECT")
    @Transactional
    public TestDriveResponse reject(CurrentUser seller, Long testDriveId, String reason) {
        TestDrive testDrive = lockOrThrow(testDriveId);
        requireOwningSeller(seller, testDrive);

        if (testDrive.getStatus() != TestDriveStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Cannot reject a test drive in status " + testDrive.getStatus() + "; only PENDING -> REJECTED is allowed");
        }

        testDrive.setStatus(TestDriveStatus.REJECTED);
        testDrive.setRejectionReason(reason);
        testDrive = testDriveRepository.save(testDrive);
        recordOutboxEvent(testDrive, "TEST_DRIVE_REJECTED");
        return TestDriveResponse.from(testDrive);
    }

    @Audited("TEST_DRIVE_CANCEL")
    @Transactional
    public TestDriveResponse cancel(CurrentUser user, Long testDriveId, String reason) {
        TestDrive testDrive = lockOrThrow(testDriveId);

        boolean isOwningBuyer = user.isBuyer() && testDrive.getBuyerUsername().equals(user.getUsername());
        boolean isOwningSeller = user.isSeller() && testDrive.getSellerUsername().equals(user.getUsername());
        if (!isOwningBuyer && !isOwningSeller) {
            throw new ForbiddenException("Only the buyer or seller on this test drive may cancel it");
        }

        if (testDrive.getStatus() != TestDriveStatus.PENDING && testDrive.getStatus() != TestDriveStatus.CONFIRMED) {
            throw new InvalidStateTransitionException(
                    "Cannot cancel a test drive in status " + testDrive.getStatus() +
                            "; only PENDING -> CANCELLED or CONFIRMED -> CANCELLED are allowed");
        }

        testDrive.setStatus(TestDriveStatus.CANCELLED);
        testDrive.setCancellationReason(reason);
        testDrive = testDriveRepository.save(testDrive);
        recordOutboxEvent(testDrive, "TEST_DRIVE_CANCELLED");
        return TestDriveResponse.from(testDrive);
    }

    @Audited("TEST_DRIVE_RESCHEDULE")
    @Transactional
    public TestDriveResponse reschedule(CurrentUser user, Long testDriveId, RescheduleRequest request) {
        TestDrive testDrive = lockOrThrow(testDriveId);

        boolean isOwningBuyer = user.isBuyer() && testDrive.getBuyerUsername().equals(user.getUsername());
        boolean isOwningSeller = user.isSeller() && testDrive.getSellerUsername().equals(user.getUsername());
        if (!isOwningBuyer && !isOwningSeller) {
            throw new ForbiddenException("Only the buyer or seller on this test drive may reschedule it");
        }

        if (testDrive.getStatus() != TestDriveStatus.PENDING && testDrive.getStatus() != TestDriveStatus.CONFIRMED) {
            throw new InvalidStateTransitionException(
                    "Cannot reschedule a test drive in status " + testDrive.getStatus());
        }

        int durationMinutes = resolveDuration(testDrive.getCarId());
        LocalDateTime newStart = request.startTime();
        LocalDateTime newEnd = newStart.plusMinutes(durationMinutes);

        if (!newStart.isAfter(LocalDateTime.now())) {
            throw new ValidationException("startTime must be in the future");
        }

        validateWithinAvailability(testDrive.getCarId(), newStart, newEnd);

        List<TestDrive> overlapping = testDriveRepository.findOverlappingExcludingSelf(
                testDrive.getCarId(), testDrive.getId(), newStart, newEnd);
        if (!overlapping.isEmpty()) {
            throw new ConflictException("Requested new time overlaps an existing pending/confirmed test drive for this car");
        }

        // Atomic: this is a single UPDATE on the existing row within the current
        // transaction - never a separate cancel+create. The same EXCLUDE constraint
        // that protects INSERTs also protects this UPDATE, so no other transaction can
        // sneak an overlapping booking in between "check" and "write" here either.
        testDrive.setStartTime(newStart);
        testDrive.setEndTime(newEnd);
        testDrive.setStatus(TestDriveStatus.PENDING); // re-confirmation required after reschedule
        testDrive = testDriveRepository.save(testDrive);
        recordOutboxEvent(testDrive, "TEST_DRIVE_RESCHEDULED");
        return TestDriveResponse.from(testDrive);
    }

    @Transactional(readOnly = true)
    public List<TestDriveResponse> getMine(CurrentUser user) {
        if (user.isBuyer()) {
            return testDriveRepository.findByBuyerUsernameOrderByStartTimeDesc(user.getUsername()).stream()
                    .map(TestDriveResponse::from).collect(Collectors.toList());
        }
        if (user.isSeller()) {
            return testDriveRepository.findBySellerUsernameOrderByStartTimeDesc(user.getUsername()).stream()
                    .map(TestDriveResponse::from).collect(Collectors.toList());
        }
        throw new ForbiddenException("Unsupported role for this endpoint");
    }

    @Transactional(readOnly = true)
    public List<TestDriveResponse> getAllForAdmin(CurrentUser user) {
        if (!user.isAdmin()) {
            throw new ForbiddenException("Only admins can view all test drives");
        }
        return testDriveRepository.findAll().stream()
                .map(TestDriveResponse::from)
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------- helpers

    private TestDrive lockOrThrow(Long testDriveId) {
        return testDriveRepository.findByIdForUpdate(testDriveId)
                .orElseThrow(() -> new NotFoundException("Test drive " + testDriveId + " not found"));
    }

    private void requireOwningSeller(CurrentUser seller, TestDrive testDrive) {
        if (!seller.isSeller() || !testDrive.getSellerUsername().equals(seller.getUsername())) {
            throw new ForbiddenException("Only the seller who owns this car may perform this action");
        }
    }

    private int resolveDuration(Long carId) {
        return carScheduleConfigRepository.findById(carId)
                .map(CarScheduleConfig::getDurationMinutes)
                .orElseThrow(() -> new ValidationException(
                        "Car " + carId + " has no test-drive duration configured; the seller must create availability first"));
    }

    private void validateWithinAvailability(Long carId, LocalDateTime startTime, LocalDateTime endTime) {
        if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
            throw new ValidationException("Requested test drive must not span across midnight");
        }

        var dayOfWeek = startTime.getDayOfWeek();
        LocalTime requestedStart = startTime.toLocalTime();
        LocalTime requestedEnd = endTime.toLocalTime();

        List<Availability> windows = availabilityRepository.findByCarIdAndDayOfWeekAndActiveTrue(carId, dayOfWeek);
        if (windows.isEmpty()) {
            throw new ValidationException("Seller has no availability configured for this car on " + dayOfWeek);
        }

        boolean fitsWithinAWindow = windows.stream().anyMatch(w ->
                !requestedStart.isBefore(w.getStartTime()) && !requestedEnd.isAfter(w.getEndTime()));

        if (!fitsWithinAWindow) {
            throw new ValidationException(
                    "Requested interval " + requestedStart + "-" + requestedEnd +
                            " does not fit entirely within the seller's availability for " + dayOfWeek);
        }
    }

    private void recordOutboxEvent(TestDrive testDrive, String eventType) {
        outboxEventRepository.save(outboxEventFactory.build(testDrive, eventType));
    }
}
