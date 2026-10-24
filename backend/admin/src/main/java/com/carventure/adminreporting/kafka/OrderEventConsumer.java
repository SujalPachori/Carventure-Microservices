package com.carventure.adminreporting.kafka;

import com.carventure.adminreporting.event.OrderCompletedEvent;
import com.carventure.adminreporting.event.OrderCreatedEvent;
import com.carventure.adminreporting.model.ReportType;
import com.carventure.adminreporting.service.ReportMetricUpdater;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final ReportMetricUpdater reportMetricUpdater;
    private final ConversionTrackingService conversionTrackingService;

    @KafkaListener(topics = "order.created", groupId = "reporting-service")
    public void onOrderCreated(OrderCreatedEvent event) {
        reportMetricUpdater.increment(ReportType.SALES, "totalOrders", 1);
        reportMetricUpdater.increment(ReportType.SALES, "ordersByStatus." + event.getStatus(), 1);
    }

    @KafkaListener(topics = "order.completed", groupId = "reporting-service")
    public void onOrderCompleted(OrderCompletedEvent event) {
        reportMetricUpdater.increment(ReportType.SALES, "completedOrders", 1);
        reportMetricUpdater.increment(ReportType.SALES, "totalRevenue", event.getAmount());

        // if this car had a test drive on record, count it as a conversion
        conversionTrackingService.markConvertedIfTestDriveExists(event.getCarId());
    }
}
