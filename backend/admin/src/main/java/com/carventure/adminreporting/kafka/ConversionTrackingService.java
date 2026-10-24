package com.carventure.adminreporting.kafka;

import com.carventure.adminreporting.model.ReportType;
import com.carventure.adminreporting.service.ReportMetricUpdater;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Backs the test-drive -> purchase conversion rate KPI.
 * TestDriveEventConsumer calls recordTestDrive() when a test drive is booked;
 * OrderEventConsumer calls markConvertedIfTestDriveExists() when an order completes.
 * If the completed car has a tracked test drive, that's a conversion.
 */
@Service
@RequiredArgsConstructor
public class ConversionTrackingService {

    private final CarTestDriveTrackerRepository carTestDriveTrackerRepository;
    private final ReportMetricUpdater reportMetricUpdater;

    public void recordTestDrive(Long carId) {
        if (!carTestDriveTrackerRepository.existsById(carId)) {
            CarTestDriveTracker tracker = new CarTestDriveTracker();
            tracker.setCarId(carId);
            carTestDriveTrackerRepository.save(tracker);
        }
    }

    public void markConvertedIfTestDriveExists(Long carId) {
        if (carTestDriveTrackerRepository.existsById(carId)) {
            reportMetricUpdater.increment(ReportType.INSPECTIONS, "testDriveConversions", 1);
            // remove so a second order for the same car (unlikely, but resale) doesn't double count
            carTestDriveTrackerRepository.deleteById(carId);
        }
    }
}
