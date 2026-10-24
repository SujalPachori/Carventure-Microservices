package com.cardrive.scheduling.service;

import com.cardrive.scheduling.client.CarLookupService;
import com.cardrive.scheduling.client.dto.CarDTO;
import com.cardrive.scheduling.domain.entity.Availability;
import com.cardrive.scheduling.dto.AvailabilityResponse;
import com.cardrive.scheduling.dto.CreateAvailabilityRequest;
import com.cardrive.scheduling.dto.UpdateAvailabilityRequest;
import com.cardrive.scheduling.exception.ForbiddenException;
import com.cardrive.scheduling.exception.NotFoundException;
import com.cardrive.scheduling.exception.ValidationException;
import com.cardrive.scheduling.repository.AvailabilityRepository;
import com.cardrive.scheduling.repository.CarScheduleConfigRepository;
import com.cardrive.scheduling.security.CurrentUser;
import com.cardrive.scheduling.security.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock AvailabilityRepository availabilityRepository;
    @Mock CarScheduleConfigRepository carScheduleConfigRepository;
    @Mock CarLookupService carLookupService;

    @InjectMocks
    AvailabilityService availabilityService;

    CurrentUser seller;
    CurrentUser buyer;

    @BeforeEach
    void setUp() {
        seller = new CurrentUser("seller.jane", Set.of(Role.SELLER));
        buyer = new CurrentUser("buyer.joe", Set.of(Role.CUSTOMER));
    }

    @Test
    void buyerCannotCreateAvailability() {
        var request = new CreateAvailabilityRequest(100L, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(18, 0), null);
        assertThatThrownBy(() -> availabilityService.create(buyer, request))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(carLookupService);
    }

    @Test
    void startMustBeBeforeEnd() {
        var request = new CreateAvailabilityRequest(100L, DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(10, 0), null);
        assertThatThrownBy(() -> availabilityService.create(seller, request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void sellerCannotCreateAvailabilityForACarTheyDoNotOwn() {
        var request = new CreateAvailabilityRequest(100L, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(18, 0), null);
        when(carLookupService.getCarOrThrow(100L)).thenReturn(new CarDTO(100L, "seller.someoneElse", "Honda City", true));

        assertThatThrownBy(() -> availabilityService.create(seller, request))
                .isInstanceOf(ForbiddenException.class);
        verify(availabilityRepository, never()).save(any());
    }

    @Test
    void firstAvailabilityForACarSeedsDefaultDuration() {
        var request = new CreateAvailabilityRequest(100L, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(18, 0), null);
        when(carLookupService.getCarOrThrow(100L)).thenReturn(new CarDTO(100L, "seller.jane", "Honda City", true));
        when(carScheduleConfigRepository.findById(100L)).thenReturn(Optional.empty());
        when(availabilityRepository.save(any(Availability.class))).thenAnswer(inv -> {
            Availability a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        AvailabilityResponse response = availabilityService.create(seller, request);

        assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(response.active()).isTrue();
        assertThat(response.sellerUsername()).isEqualTo("seller.jane");
        verify(carScheduleConfigRepository).save(argThat(cfg -> cfg.getDurationMinutes() == 45));
    }

    @Test
    void explicitDurationOverridesDefaultOnFirstCreate() {
        var request = new CreateAvailabilityRequest(100L, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(18, 0), 30);
        when(carLookupService.getCarOrThrow(100L)).thenReturn(new CarDTO(100L, "seller.jane", "Honda City", true));
        when(carScheduleConfigRepository.findById(100L)).thenReturn(Optional.empty());
        when(availabilityRepository.save(any(Availability.class))).thenAnswer(inv -> inv.getArgument(0));

        availabilityService.create(seller, request);

        verify(carScheduleConfigRepository).save(argThat(cfg -> cfg.getDurationMinutes() == 30));
    }

    @Test
    void updateRejectsInvertedWindow() {
        Availability existing = Availability.builder().id(1L).carId(100L).sellerUsername("seller.jane")
                .dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(18, 0)).active(true).build();
        when(availabilityRepository.findById(1L)).thenReturn(Optional.of(existing));

        var request = new UpdateAvailabilityRequest(LocalTime.of(19, 0), LocalTime.of(9, 0), null);
        assertThatThrownBy(() -> availabilityService.update(seller, 1L, request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void nonOwningSellerCannotUpdate() {
        Availability existing = Availability.builder().id(1L).carId(100L).sellerUsername("seller.someoneElse")
                .dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(18, 0)).active(true).build();
        when(availabilityRepository.findById(1L)).thenReturn(Optional.of(existing));

        var request = new UpdateAvailabilityRequest(null, null, false);
        assertThatThrownBy(() -> availabilityService.update(seller, 1L, request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deactivateSetsActiveFalse() {
        Availability existing = Availability.builder().id(1L).carId(100L).sellerUsername("seller.jane")
                .dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(18, 0)).active(true).build();
        when(availabilityRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(availabilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        availabilityService.deactivate(seller, 1L);

        verify(availabilityRepository).save(argThat(a -> !a.isActive()));
    }

    @Test
    void deactivateMissingWindowIs404() {
        when(availabilityRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> availabilityService.deactivate(seller, 1L))
                .isInstanceOf(NotFoundException.class);
    }
}
