package com.cardrive.scheduling.dto;

import com.cardrive.scheduling.domain.entity.Availability;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record AvailabilityResponse(
        Long id,
        Long carId,
        String sellerUsername,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean active
) {
    public static AvailabilityResponse from(Availability a) {
        return new AvailabilityResponse(
                a.getId(), a.getCarId(), a.getSellerUsername(), a.getDayOfWeek(),
                a.getStartTime(), a.getEndTime(), a.isActive());
    }
}
