package com.cardrive.scheduling.service;

import com.cardrive.scheduling.client.CarLookupService;
import com.cardrive.scheduling.client.dto.CarDTO;
import com.cardrive.scheduling.domain.entity.Availability;
import com.cardrive.scheduling.domain.entity.CarScheduleConfig;
import com.cardrive.scheduling.domain.entity.TestDrive;
import com.cardrive.scheduling.domain.entity.TestDriveStatus;
import com.cardrive.scheduling.dto.CreateTestDriveRequest;
import com.cardrive.scheduling.dto.RescheduleRequest;
import com.cardrive.scheduling.dto.TestDriveResponse;
import com.cardrive.scheduling.exception.*;
import com.cardrive.scheduling.outbox.OutboxEventFactory;
import com.cardrive.scheduling.repository.AvailabilityRepository;
import com.cardrive.scheduling.repository.CarScheduleConfigRepository;
import com.cardrive.scheduling.repository.OutboxEventRepository;
import com.cardrive.scheduling.repository.TestDriveRepository;
import com.cardrive.scheduling.security.CurrentUser;
import com.cardrive.scheduling.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestDriveServiceTest {

    @Mock TestDriveRepository testDriveRepository;
    @Mock AvailabilityRepository availabilityRepository;
    @Mock CarScheduleConfigRepository carScheduleConfigRepository;
    @Mock OutboxEventRepository outboxEventRepository;
    @Mock OutboxEventFactory outboxEventFactory;
    @Mock CarLookupService carLookupService;

    @InjectMocks
    TestDriveService testDriveService;

    CurrentUser buyer;
    CurrentUser seller;
    CurrentUser otherSeller;
    CurrentUser admin;

    // A future Tuesday, well clear of "now" regardless of when the suite runs.
    LocalDateTime futureStart;

    @BeforeEach
    void setUp() {
        buyer = new CurrentUser("buyer.joe", Set.of(Role.CUSTOMER));
        seller = new CurrentUser("seller.jane", Set.of(Role.SELLER));
        otherSeller = new CurrentUser("seller.other", Set.of(Role.SELLER));
        admin = new CurrentUser("admin.root", Set.of(Role.ADMIN));

        futureStart = nextDayOfWeek(DayOfWeek.TUESDAY).atTime(14, 30);
    }

    private java.time.LocalDate nextDayOfWeek(DayOfWeek target) {
        java.time.LocalDate date = java.time.LocalDate.now().plusDays(14); // comfortably in the future
        while (date.getDayOfWeek() != target) {
            date = date.plusDays(1);
        }
        return date;
    }

    private CarDTO activeCar(long carId, String sellerUsername) {
        return new CarDTO(carId, sellerUsername, "Honda City", true);
    }

    private CarScheduleConfig config(long carId, String sellerUsername, int duration) {
        return CarScheduleConfig.builder().carId(carId).sellerUsername(sellerUsername).durationMinutes(duration).build();
    }

    private Availability window(long carId, String sellerUsername, DayOfWeek day, LocalTime start, LocalTime end) {
        return Availability.builder().carId(carId).sellerUsername(sellerUsername).dayOfWeek(day)
                .startTime(start).endTime(end).active(true).build();
    }

    @Nested
    class Create {

        @Test
        void nonBuyerIsForbidden() {
            var request = new CreateTestDriveRequest(100L, futureStart);
            assertThatThrownBy(() -> testDriveService.create(seller, request))
                    .isInstanceOf(ForbiddenException.class);
            verifyNoInteractions(carLookupService, testDriveRepository);
        }

        @Test
        void happyPathCalculatesEndTimeAndSaves() {
            var request = new CreateTestDriveRequest(100L, futureStart);
            when(carLookupService.getCarOrThrow(100L)).thenReturn(activeCar(100L, "seller.jane"));
            when(carScheduleConfigRepository.findById(100L)).thenReturn(Optional.of(config(100L, "seller.jane", 45)));
            when(availabilityRepository.findByCarIdAndDayOfWeekAndActiveTrue(100L, DayOfWeek.TUESDAY))
                    .thenReturn(List.of(window(100L, "seller.jane", DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(18, 0))));
            when(testDriveRepository.findOverlapping(eq(100L), any(), any())).thenReturn(List.of());
            when(testDriveRepository.save(any(TestDrive.class))).thenAnswer(inv -> {
                TestDrive t = inv.getArgument(0);
                t.setId(1L);
                return t;
            });

            TestDriveResponse response = testDriveService.create(buyer, request);

            assertThat(response.status()).isEqualTo(TestDriveStatus.PENDING);
            assertThat(response.startTime()).isEqualTo(futureStart);
            assertThat(response.endTime()).isEqualTo(futureStart.plusMinutes(45));
            assertThat(response.sellerUsername()).isEqualTo("seller.jane"); // resolved from Car Discovery, not the client
            assertThat(response.buyerUsername()).isEqualTo("buyer.joe");
            verify(outboxEventRepository).save(any());
        }

        @Test
        void pastStartTimeIsRejected() {
            var request = new CreateTestDriveRequest(100L, LocalDateTime.now().minusHours(1));
            when(carLookupService.getCarOrThrow(100L)).thenReturn(activeCar(100L, "seller.jane"));
            when(carScheduleConfigRepository.findById(100L)).thenReturn(Optional.of(config(100L, "seller.jane", 45)));

            assertThatThrownBy(() -> testDriveService.create(buyer, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("future");
        }

        @Test
        void outsideSellerAvailabilityIsRejected() {
            var request = new CreateTestDriveRequest(100L, futureStart);
            when(carLookupService.getCarOrThrow(100L)).thenReturn(activeCar(100L, "seller.jane"));
            when(carScheduleConfigRepository.findById(100L)).thenReturn(Optional.of(config(100L, "seller.jane", 45)));
            when(availabilityRepository.findByCarIdAndDayOfWeekAndActiveTrue(100L, DayOfWeek.TUESDAY))
                    .thenReturn(List.of()); // no windows configured for this day

            assertThatThrownBy(() -> testDriveService.create(buyer, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("availability");
        }

        @Test
        void partiallyOutsideWindowIsRejected() {
            // window ends 15:00, requested 14:30-15:15 (45 min) spills over
            var request = new CreateTestDriveRequest(100L, futureStart);
            when(carLookupService.getCarOrThrow(100L)).thenReturn(activeCar(100L, "seller.jane"));
            when(carScheduleConfigRepository.findById(100L)).thenReturn(Optional.of(config(100L, "seller.jane", 45)));
            when(availabilityRepository.findByCarIdAndDayOfWeekAndActiveTrue(100L, DayOfWeek.TUESDAY))
                    .thenReturn(List.of(window(100L, "seller.jane", DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(15, 0))));

            assertThatThrownBy(() -> testDriveService.create(buyer, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("does not fit entirely");
        }

        @Test
        void applicationLevelOverlapPreCheckThrowsConflict() {
            var request = new CreateTestDriveRequest(100L, futureStart);
            when(carLookupService.getCarOrThrow(100L)).thenReturn(activeCar(100L, "seller.jane"));
            when(carScheduleConfigRepository.findById(100L)).thenReturn(Optional.of(config(100L, "seller.jane", 45)));
            when(availabilityRepository.findByCarIdAndDayOfWeekAndActiveTrue(100L, DayOfWeek.TUESDAY))
                    .thenReturn(List.of(window(100L, "seller.jane", DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(18, 0))));
            when(testDriveRepository.findOverlapping(eq(100L), any(), any()))
                    .thenReturn(List.of(TestDrive.builder().id(5L).build()));

            assertThatThrownBy(() -> testDriveService.create(buyer, request))
                    .isInstanceOf(ConflictException.class);
            verify(testDriveRepository, never()).save(any());
        }

        @Test
        void dbExclusionConstraintViolationSurfacesAsConflictAtTheHandlerLevel() {
            // The service itself just lets DataIntegrityViolationException propagate;
            // GlobalExceptionHandler is what turns it into a 409. This test documents
            // that the service does NOT swallow or reinterpret it.
            var request = new CreateTestDriveRequest(100L, futureStart);
            when(carLookupService.getCarOrThrow(100L)).thenReturn(activeCar(100L, "seller.jane"));
            when(carScheduleConfigRepository.findById(100L)).thenReturn(Optional.of(config(100L, "seller.jane", 45)));
            when(availabilityRepository.findByCarIdAndDayOfWeekAndActiveTrue(100L, DayOfWeek.TUESDAY))
                    .thenReturn(List.of(window(100L, "seller.jane", DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(18, 0))));
            when(testDriveRepository.findOverlapping(eq(100L), any(), any())).thenReturn(List.of());
            when(testDriveRepository.save(any(TestDrive.class)))
                    .thenThrow(new org.springframework.dao.DataIntegrityViolationException("exclusion violation"));

            assertThatThrownBy(() -> testDriveService.create(buyer, request))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        }
    }

    @Nested
    class Confirm {

        @Test
        void ownerSellerCanConfirmPending() {
            TestDrive existing = TestDrive.builder().id(1L).carId(100L).sellerUsername("seller.jane").buyerUsername("buyer.joe")
                    .startTime(futureStart).endTime(futureStart.plusMinutes(45))
                    .status(TestDriveStatus.PENDING).build();
            when(testDriveRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
            when(testDriveRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TestDriveResponse response = testDriveService.confirm(seller, 1L);

            assertThat(response.status()).isEqualTo(TestDriveStatus.CONFIRMED);
            verify(outboxEventRepository).save(any());
        }

        @Test
        void nonOwningSellerIsForbidden() {
            TestDrive existing = TestDrive.builder().id(1L).carId(100L).sellerUsername("seller.jane").buyerUsername("buyer.joe")
                    .status(TestDriveStatus.PENDING).build();
            when(testDriveRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> testDriveService.confirm(otherSeller, 1L))
                    .isInstanceOf(ForbiddenException.class);
            verify(testDriveRepository, never()).save(any());
        }

        @Test
        void alreadyConfirmedCannotBeConfirmedAgain() {
            TestDrive existing = TestDrive.builder().id(1L).carId(100L).sellerUsername("seller.jane").buyerUsername("buyer.joe")
                    .status(TestDriveStatus.CONFIRMED).build();
            when(testDriveRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> testDriveService.confirm(seller, 1L))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        void rejectedCannotTransitionToConfirmed() {
            TestDrive existing = TestDrive.builder().id(1L).carId(100L).sellerUsername("seller.jane").buyerUsername("buyer.joe")
                    .status(TestDriveStatus.REJECTED).build();
            when(testDriveRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> testDriveService.confirm(seller, 1L))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        void missingTestDriveIs404() {
            when(testDriveRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> testDriveService.confirm(seller, 999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    class CancelAndReschedule {

        @Test
        void owningBuyerCanCancelPending() {
            TestDrive existing = TestDrive.builder().id(1L).carId(100L).sellerUsername("seller.jane").buyerUsername("buyer.joe")
                    .status(TestDriveStatus.PENDING).build();
            when(testDriveRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
            when(testDriveRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TestDriveResponse response = testDriveService.cancel(buyer, 1L, "change of plans");

            assertThat(response.status()).isEqualTo(TestDriveStatus.CANCELLED);
            assertThat(response.cancellationReason()).isEqualTo("change of plans");
        }

        @Test
        void unrelatedBuyerCannotCancel() {
            TestDrive existing = TestDrive.builder().id(1L).carId(100L).sellerUsername("seller.jane").buyerUsername("buyer.joe")
                    .status(TestDriveStatus.PENDING).build();
            when(testDriveRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
            CurrentUser otherBuyer = new CurrentUser("buyer.someoneElse", Set.of(Role.CUSTOMER));

            assertThatThrownBy(() -> testDriveService.cancel(otherBuyer, 1L, null))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void aBuyerWhoIsAlsoASellerCanStillCancelTheirOwnBooking() {
            // auth-service allows a user to hold CUSTOMER and SELLER simultaneously.
            TestDrive existing = TestDrive.builder().id(1L).carId(100L).sellerUsername("seller.jane").buyerUsername("dual.role.user")
                    .status(TestDriveStatus.PENDING).build();
            when(testDriveRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
            when(testDriveRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CurrentUser dualRoleUser = new CurrentUser("dual.role.user", EnumSet.of(Role.CUSTOMER, Role.SELLER));

            TestDriveResponse response = testDriveService.cancel(dualRoleUser, 1L, null);

            assertThat(response.status()).isEqualTo(TestDriveStatus.CANCELLED);
        }

        @Test
        void completedCannotBeCancelled() {
            TestDrive existing = TestDrive.builder().id(1L).carId(100L).sellerUsername("seller.jane").buyerUsername("buyer.joe")
                    .status(TestDriveStatus.COMPLETED).build();
            when(testDriveRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> testDriveService.cancel(buyer, 1L, null))
                    .isInstanceOf(InvalidStateTransitionException.class);
        }

        @Test
        void rescheduleIsAtomicUpdateAndResetsToPending() {
            LocalDateTime newStart = futureStart.plusDays(7); // still a Tuesday
            TestDrive existing = TestDrive.builder().id(1L).carId(100L).sellerUsername("seller.jane").buyerUsername("buyer.joe")
                    .startTime(futureStart).endTime(futureStart.plusMinutes(45))
                    .status(TestDriveStatus.CONFIRMED).build();

            when(testDriveRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
            when(carScheduleConfigRepository.findById(100L)).thenReturn(Optional.of(config(100L, "seller.jane", 45)));
            when(availabilityRepository.findByCarIdAndDayOfWeekAndActiveTrue(100L, DayOfWeek.TUESDAY))
                    .thenReturn(List.of(window(100L, "seller.jane", DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(18, 0))));
            when(testDriveRepository.findOverlappingExcludingSelf(eq(100L), eq(1L), any(), any()))
                    .thenReturn(List.of());
            when(testDriveRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TestDriveResponse response = testDriveService.reschedule(seller, 1L, new RescheduleRequest(newStart));

            assertThat(response.startTime()).isEqualTo(newStart);
            assertThat(response.endTime()).isEqualTo(newStart.plusMinutes(45));
            assertThat(response.status()).isEqualTo(TestDriveStatus.PENDING);
            // exactly one save - a single atomic update, never cancel-then-recreate
            verify(testDriveRepository, times(1)).save(any());
        }

        @Test
        void rescheduleOverlapIsRejected() {
            LocalDateTime newStart = futureStart.plusDays(7);
            TestDrive existing = TestDrive.builder().id(1L).carId(100L).sellerUsername("seller.jane").buyerUsername("buyer.joe")
                    .startTime(futureStart).endTime(futureStart.plusMinutes(45))
                    .status(TestDriveStatus.PENDING).build();

            when(testDriveRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
            when(carScheduleConfigRepository.findById(100L)).thenReturn(Optional.of(config(100L, "seller.jane", 45)));
            when(availabilityRepository.findByCarIdAndDayOfWeekAndActiveTrue(100L, DayOfWeek.TUESDAY))
                    .thenReturn(List.of(window(100L, "seller.jane", DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(18, 0))));
            when(testDriveRepository.findOverlappingExcludingSelf(eq(100L), eq(1L), any(), any()))
                    .thenReturn(List.of(TestDrive.builder().id(2L).build()));

            assertThatThrownBy(() -> testDriveService.reschedule(buyer, 1L, new RescheduleRequest(newStart)))
                    .isInstanceOf(ConflictException.class);
            verify(testDriveRepository, never()).save(any());
        }
    }

    @Nested
    class AdminView {

        @Test
        void nonAdminForbiddenFromViewingAll() {
            assertThatThrownBy(() -> testDriveService.getAllForAdmin(seller))
                    .isInstanceOf(ForbiddenException.class);
            verifyNoInteractions(testDriveRepository);
        }

        @Test
        void adminCanViewAll() {
            when(testDriveRepository.findAll()).thenReturn(List.of(
                    TestDrive.builder().id(1L).carId(100L).sellerUsername("seller.jane").buyerUsername("buyer.joe")
                            .status(TestDriveStatus.PENDING).build()
            ));
            List<TestDriveResponse> all = testDriveService.getAllForAdmin(admin);
            assertThat(all).hasSize(1);
        }
    }
}
