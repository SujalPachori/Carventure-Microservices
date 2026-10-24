package com.cardrive.scheduling.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateTestDriveRequest(
        @NotNull Long carId,
        @NotNull LocalDateTime startTime
) {
}
