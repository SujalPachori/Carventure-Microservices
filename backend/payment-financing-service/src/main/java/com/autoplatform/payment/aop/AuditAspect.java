package com.autoplatform.payment.aop;

import com.autoplatform.payment.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Financial audit trail: every order state change (created, paid, financing
 * activated, failed, cancelled, refunded) gets a structured log line here,
 * independent of the general execution-timing concern in LoggingAspect.
 */
@Aspect
@Component
@Slf4j
public class AuditAspect {

    @AfterReturning(
            pointcut = "execution(public com.autoplatform.payment.entity.Order com.autoplatform.payment.service.OrderService.*(..))",
            returning = "order")
    public void auditOrderChange(Order order) {
        log.info("AUDIT orderId={} carId={} buyerId={} sellerId={} status={} totalAmount={} {}",
                order.getId(), order.getCarId(), order.getBuyerId(), order.getSellerId(),
                order.getStatus(), order.getTotalAmount(), order.getCurrency());
    }
}
