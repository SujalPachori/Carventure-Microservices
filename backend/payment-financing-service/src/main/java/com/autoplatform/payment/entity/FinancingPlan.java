package com.autoplatform.payment.entity;

import com.autoplatform.payment.enums.FinancingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Simplified in-house financing plan created once the down payment succeeds.
 * Interest-free equal installments for V1 - deliberately not a full loan-
 * servicing engine; swap in a real financing partner integration later
 * without changing the Order/Payment model (see README).
 */
@Entity
@Table(name = "financing_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "principal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    @Column(name = "monthly_installment", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyInstallment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FinancingStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
