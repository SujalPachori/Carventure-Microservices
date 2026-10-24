package com.cardrive.scheduling.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Per-car test-drive configuration owned by the Scheduling Service (Car Discovery
 * Service owns the car's catalog data, not its test-drive duration).
 * Upserted the first time a seller creates availability for a car; the seller
 * may update it afterwards.
 */
@Entity
@Table(name = "car_schedule_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarScheduleConfig {

    @Id
    @Column(name = "car_id")
    private Long carId;

    @Column(name = "seller_username", nullable = false, length = 100)
    private String sellerUsername;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;
}
