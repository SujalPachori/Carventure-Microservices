package com.cardrive.scheduling.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * A single test-drive request/booking.
 *
 * Concurrency safety for overlapping bookings on the same car is NOT enforced here
 * in application code alone - the authoritative guarantee is the PostgreSQL partial
 * EXCLUDE USING gist constraint declared in the V2 Flyway migration, which rejects
 * any INSERT/UPDATE that would create two rows for the same car_id with overlapping
 * [start_time, end_time) ranges while both rows are in an active status
 * (PENDING or CONFIRMED). The application-level conflict check that happens before
 * INSERT is only a fast-path / better-error-message optimization.
 */
@Entity
@Table(name = "test_drive")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestDrive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "car_id", nullable = false)
    private Long carId;

    @Column(name = "seller_username", nullable = false, length = 100)
    private String sellerUsername;

    @Column(name = "buyer_username", nullable = false, length = 100)
    private String buyerUsername;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TestDriveStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic lock as a secondary safety net on top of the pessimistic read-lock used for transitions. */
    @Version
    private Long version;
}
