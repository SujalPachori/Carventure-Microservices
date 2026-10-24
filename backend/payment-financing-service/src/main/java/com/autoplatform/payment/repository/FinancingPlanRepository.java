package com.autoplatform.payment.repository;

import com.autoplatform.payment.entity.FinancingPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FinancingPlanRepository extends JpaRepository<FinancingPlan, Long> {

    Optional<FinancingPlan> findByOrderId(Long orderId);
}
