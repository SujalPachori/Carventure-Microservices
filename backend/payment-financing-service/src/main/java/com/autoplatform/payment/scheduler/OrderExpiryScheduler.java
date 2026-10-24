package com.autoplatform.payment.scheduler;

import com.autoplatform.payment.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryScheduler {

    private final OrderService orderService;

    @Scheduled(
            fixedDelayString = "${payment.order-expiry.sweep-interval-ms:300000}",
            initialDelayString = "${payment.order-expiry.sweep-initial-delay-ms:60000}")
    public void sweep() {
        int expired = orderService.expireStaleOrders();
        if (expired > 0) {
            log.info("Order expiry sweep expired {} stale order(s)", expired);
        }
    }
}