package com.carventure.adminreporting.kafka;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

// One row per carId that has at least one scheduled test drive on record.
// Used only to answer "did this car have a test drive?" when an order completes -
// not meant to be a full test-drive history (Scheduling service owns that).
@Entity
@Data
public class CarTestDriveTracker {
    @Id
    private Long carId;
}
