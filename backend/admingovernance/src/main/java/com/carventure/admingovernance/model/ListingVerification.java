package com.carventure.admingovernance.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * This service doesn't own car data (Car service does), so this is an audit
 * trail of verification decisions only. The actual Car.status field is
 * updated on the Car service side via Feign — see CarServiceClient.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "listing_verification")
public class ListingVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "car_id", nullable = false)
    private Long carId;

    @Enumerated(EnumType.STRING)
    private VerificationDecision decision;

    private String reason;

    @Column(name = "verified_by_admin_username")
    private String verifiedByAdminUsername;

    @Column(name = "verified_date")
    private LocalDateTime verifiedDate;
}