package com.carventure.adminreporting.kafka;

import com.carventure.adminreporting.event.CarListedEvent;
import com.carventure.adminreporting.model.ReportType;
import com.carventure.adminreporting.service.ReportMetricUpdater;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CarEventConsumer {

    private final ReportMetricUpdater reportMetricUpdater;

    @KafkaListener(topics = "car.listed", groupId = "reporting-service")
    public void onCarListed(CarListedEvent event) {
        reportMetricUpdater.increment(ReportType.INVENTORY, "totalListings", 1);
        reportMetricUpdater.increment(ReportType.INVENTORY, "listingsByBrand." + event.getBrand(), 1);
    }
}
