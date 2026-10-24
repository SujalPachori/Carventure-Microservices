package com.carventure.admingovernance.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "dispute")
public class Dispute {

    @Id
    @Column(name = "dispute_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long disputeId;

    @Column(name = "car_id", nullable = false)
    private Long carId;

    @Column(name = "buyer_username", nullable = false)
    private String buyerUsername;

    @Column(name = "seller_username", nullable = false)
    private String sellerUsername;

    @Enumerated(EnumType.STRING)
    private DisputeStatus status;

    @Column(name = "resolution_notes")
    private String resolutionNotes;

    @Column(name = "resolved_by_admin_username")
    private String resolvedByAdminUsername;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "resolved_date")
    private LocalDateTime resolvedDate;
}