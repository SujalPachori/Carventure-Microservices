package com.cardrive.scheduling.schedulingkafka.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestDriveCompletedEvent {
    private Long testDriveId;
    private Long carId;
    private String buyerUsername;
    private LocalDateTime completedDate;
}
