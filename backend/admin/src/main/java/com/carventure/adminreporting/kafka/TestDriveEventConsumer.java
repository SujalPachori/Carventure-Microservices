package com.carventure.adminreporting.kafka;

import com.carventure.adminreporting.event.TestDriveCompletedEvent;
import com.carventure.adminreporting.event.TestDriveScheduledEvent;
import com.carventure.adminreporting.model.ReportType;
import com.carventure.adminreporting.service.ReportMetricUpdater;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TestDriveEventConsumer {

    private final ReportMetricUpdater reportMetricUpdater;
    private final ConversionTrackingService conversionTrackingService;

    @KafkaListener(topics = "testdrive.scheduled", groupId = "reporting-service")
    public void onTestDriveScheduled(TestDriveScheduledEvent event) {
        reportMetricUpdater.increment(ReportType.INSPECTIONS, "totalTestDrives",1 );
        // remember this carId so OrderEventConsumer can check it later for conversion
        conversionTrackingService.recordTestDrive(event.getCarId());
    }

    @KafkaListener(topics = "testdrive.completed", groupId = "reporting-service")
    public void onTestDriveCompleted(TestDriveCompletedEvent event) {
        reportMetricUpdater.increment(ReportType.INSPECTIONS, "completedTestDrives",1 );
    }
}
