package com.carventure.adminreporting.service;

import com.carventure.adminreporting.dto.ReportDTO;
import com.carventure.adminreporting.model.ReportMetric;
import com.carventure.adminreporting.model.ReportType;
import com.carventure.adminreporting.repository.ReportMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportingServiceImpl implements ReportingService {

    private final ReportMetricRepository reportMetricRepository;

    @Override
    public List<ReportDTO> viewReports() {
        List<ReportDTO> reports = new ArrayList<>();

        for (ReportType reportType : ReportType.values()) {
            List<ReportMetric> metricsForType = reportMetricRepository.findByReportType(reportType);

            Map<String, Double> metricsMap = new HashMap<>();
            for (ReportMetric metric : metricsForType) {
                metricsMap.put(metric.getMetricKey(), metric.getMetricValue());
            }

            reports.add(new ReportDTO(reportType, metricsMap));
        }

        return reports;
    }
}
