package com.cardrive.scheduling.exception;

/**
 * Raised when a test-drive request collides with another active booking for the
 * same car - either detected by the application-level pre-check or surfaced from
 * a PostgreSQL exclusion-constraint violation caught at commit time.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
