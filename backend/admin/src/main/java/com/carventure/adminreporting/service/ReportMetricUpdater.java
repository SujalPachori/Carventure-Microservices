package com.carventure.adminreporting.service;

import com.carventure.adminreporting.model.ReportMetric;
import com.carventure.adminreporting.model.ReportType;
import com.carventure.adminreporting.repository.ReportMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReportMetricUpdater {

    private final ReportMetricRepository reportMetricRepository;

    /**
     * Adds `amount` to the current value of (reportType, metricKey), creating
     * the row with amount as its starting value if this is the first time
     * this metric has been touched. The update itself happens as a single
     * atomic SQL statement so concurrent Kafka messages for the same metric
     * can't clobber each other (no read-modify-write race).
     */
    @Transactional
    public void increment(ReportType reportType, String metricKey, double amount) {
        LocalDateTime now = LocalDateTime.now();
        int rowsUpdated = reportMetricRepository.incrementIfExists(reportType, metricKey, amount, now);

        if (rowsUpdated == 0) {
            // first time this metric has been touched — insert it.
            // findByReportTypeAndMetricKey guard covers the (rare) race where
            // two threads both see "0 rows updated" for a brand-new metric key
            // at once; the unique constraint on (report_type, metric_key)
            // backstops it if this check itself loses a race.
            Optional<ReportMetric> stillMissing =
                    reportMetricRepository.findByReportTypeAndMetricKey(reportType, metricKey);

            if (stillMissing.isEmpty()) {
                ReportMetric metric = new ReportMetric();
                metric.setReportType(reportType);
                metric.setMetricKey(metricKey);
                metric.setMetricValue(amount);
                metric.setLastUpdated(now);
                reportMetricRepository.save(metric);
            } else {
                reportMetricRepository.incrementIfExists(reportType, metricKey, amount, now);
            }
        }
    }
}
