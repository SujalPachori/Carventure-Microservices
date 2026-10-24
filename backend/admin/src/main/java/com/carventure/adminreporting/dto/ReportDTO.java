package com.carventure.adminreporting.dto;

import com.carventure.adminreporting.model.ReportType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * One of these per report type. `metrics` is metricKey -> metricValue, e.g.
 * for POPULAR_SEARCHES that's {"suv": 57.0, "sedan": 41.0, ...}; for SALES
 * that's {"totalCount": 128.0, "totalValue": 45230000.0}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {
    private ReportType reportType;
    private Map<String, Double> metrics;
}
