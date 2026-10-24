package com.cardrive.scheduling.dto;

import com.cardrive.scheduling.domain.entity.TestDrive;
import com.cardrive.scheduling.domain.entity.TestDriveStatus;

import java.time.Instant;
import java.time.LocalDateTime;

public record TestDriveResponse(
        Long id,
        Long carId,
        String sellerUsername,
        String buyerUsername,
        LocalDateTime startTime,
        LocalDateTime endTime,
        TestDriveStatus status,
        String rejectionReason,
        String cancellationReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static TestDriveResponse from(TestDrive t) {
        return new TestDriveResponse(
                t.getId(), t.getCarId(), t.getSellerUsername(), t.getBuyerUsername(),
                t.getStartTime(), t.getEndTime(), t.getStatus(),
                t.getRejectionReason(), t.getCancellationReason(),
                t.getCreatedAt(), t.getUpdatedAt());
    }
}
