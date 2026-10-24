package com.cardrive.scheduling.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RescheduleRequest(
        @NotNull LocalDateTime startTime
) {
}
