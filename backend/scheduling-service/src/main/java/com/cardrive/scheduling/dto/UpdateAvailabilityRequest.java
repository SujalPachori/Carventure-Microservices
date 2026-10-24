package com.cardrive.scheduling.dto;

import java.time.LocalTime;

public record UpdateAvailabilityRequest(
        LocalTime startTime,
        LocalTime endTime,
        Boolean active
) {
}
