package com.cardrive.scheduling.service;

import com.cardrive.scheduling.client.CarLookupService;
import com.cardrive.scheduling.client.dto.CarDTO;
import com.cardrive.scheduling.domain.entity.Availability;
import com.cardrive.scheduling.domain.entity.CarScheduleConfig;
import com.cardrive.scheduling.dto.AvailabilityResponse;
import com.cardrive.scheduling.dto.CreateAvailabilityRequest;
import com.cardrive.scheduling.dto.UpdateAvailabilityRequest;
import com.cardrive.scheduling.exception.ForbiddenException;
import com.cardrive.scheduling.exception.NotFoundException;
import com.cardrive.scheduling.exception.ValidationException;
import com.cardrive.scheduling.repository.AvailabilityRepository;
import com.cardrive.scheduling.repository.CarScheduleConfigRepository;
import com.cardrive.scheduling.schedulingkafka.kafka.TestDriveEventProducer;
import com.cardrive.scheduling.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private static final int DEFAULT_DURATION_MINUTES = 45;

    private final AvailabilityRepository availabilityRepository;
    private final CarScheduleConfigRepository carScheduleConfigRepository;
    private final CarLookupService carLookupService;

    @Transactional
    public AvailabilityResponse create(CurrentUser seller, CreateAvailabilityRequest request) {
        requireSeller(seller);
        validateWindow(request.startTime(), request.endTime());

        CarDTO car = carLookupService.getCarOrThrow(request.carId());
        requireOwnsCar(seller, car);

        ensureScheduleConfig(request.carId(), seller.getUsername(), request.durationMinutes());

        Availability availability = Availability.builder()
                .carId(request.carId())
                .sellerUsername(seller.getUsername())
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .active(true)
                .build();

        return AvailabilityResponse.from(availabilityRepository.save(availability));
    }

    @Transactional
    public AvailabilityResponse update(CurrentUser seller, Long availabilityId, UpdateAvailabilityRequest request) {
        requireSeller(seller);
        Availability availability = getOrThrow(availabilityId);
        requireOwnsAvailability(seller, availability);

        var newStart = request.startTime() != null ? request.startTime() : availability.getStartTime();
        var newEnd = request.endTime() != null ? request.endTime() : availability.getEndTime();
        validateWindow(newStart, newEnd);

        availability.setStartTime(newStart);
        availability.setEndTime(newEnd);
        if (request.active() != null) {
            availability.setActive(request.active());
        }

        return AvailabilityResponse.from(availabilityRepository.save(availability));
    }

    @Transactional
    public void deactivate(CurrentUser seller, Long availabilityId) {
        requireSeller(seller);
        Availability availability = getOrThrow(availabilityId);
        requireOwnsAvailability(seller, availability);
        availability.setActive(false);
        availabilityRepository.save(availability);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getForCar(Long carId) {
        return availabilityRepository.findByCarIdOrderByDayOfWeekAscStartTimeAsc(carId).stream()
                .map(AvailabilityResponse::from)
                .collect(Collectors.toList());
    }

    private void ensureScheduleConfig(Long carId, String sellerUsername, Integer requestedDuration) {
        carScheduleConfigRepository.findById(carId).ifPresentOrElse(
                existing -> {
                    if (requestedDuration != null) {
                        existing.setDurationMinutes(requestedDuration);
                        carScheduleConfigRepository.save(existing);
                    }
                },
                () -> carScheduleConfigRepository.save(CarScheduleConfig.builder()
                        .carId(carId)
                        .sellerUsername(sellerUsername)
                        .durationMinutes(requestedDuration != null ? requestedDuration : DEFAULT_DURATION_MINUTES)
                        .build())
        );
    }

    private void validateWindow(java.time.LocalTime start, java.time.LocalTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new ValidationException("Availability startTime must be strictly before endTime");
        }
    }

    private Availability getOrThrow(Long id) {
        return availabilityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Availability " + id + " not found"));
    }

    private void requireSeller(CurrentUser user) {
        if (!user.isSeller()) {
            throw new ForbiddenException("Only sellers can manage availability");
        }
    }

    private void requireOwnsCar(CurrentUser seller, CarDTO car) {
        if (!car.sellerUsername().equals(seller.getUsername())) {
            throw new ForbiddenException("You do not own car " + car.id());
        }
    }

    private void requireOwnsAvailability(CurrentUser seller, Availability availability) {
        if (!availability.getSellerUsername().equals(seller.getUsername())) {
            throw new ForbiddenException("You do not own this availability window");
        }
    }
}
