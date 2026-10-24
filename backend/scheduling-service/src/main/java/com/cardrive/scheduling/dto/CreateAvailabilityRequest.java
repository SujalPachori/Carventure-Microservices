package com.cardrive.scheduling.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record CreateAvailabilityRequest(
        @NotNull Long carId,
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        Integer durationMinutes // optional: only needed the first time a car is configured; defaults to 45
) {
}
