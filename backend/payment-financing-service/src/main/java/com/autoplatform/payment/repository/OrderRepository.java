package com.autoplatform.payment.repository;

import com.autoplatform.payment.entity.Order;
import com.autoplatform.payment.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);

    List<Order> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    Optional<Order> findByCarIdAndStatusIn(Long carId, List<OrderStatus> statuses);

    /** Orders past their expiry that still block their car - the OrderExpiryScheduler sweeps these. */
    List<Order> findByStatusInAndExpiresAtBefore(List<OrderStatus> statuses, java.time.Instant cutoff);

    /** Row lock on the order being finalized by a webhook, so two webhook deliveries for the same order can't race each other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findAndLockById(@Param("id") Long id);
}
