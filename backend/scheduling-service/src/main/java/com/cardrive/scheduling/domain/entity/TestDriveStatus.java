package com.cardrive.scheduling.domain.entity;

public enum TestDriveStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    CANCELLED,
    COMPLETED;

    /** States considered "active" for the purposes of overlap/conflict detection. */
    public static boolean isActive(TestDriveStatus status) {
        return status == PENDING || status == CONFIRMED;
    }
}
