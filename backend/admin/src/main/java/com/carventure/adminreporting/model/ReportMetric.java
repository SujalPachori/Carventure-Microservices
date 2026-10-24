package com.carventure.adminreporting.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Generic aggregate row so every report type shares one table instead of a
 * separate rigid table per report. E.g.:
 *   (SALES, "totalCount")       -> 128
 *   (SALES, "totalValue")       -> 45230000.0
 *   (INVENTORY, "activeCount")  -> 342
 *   (POPULAR_SEARCHES, "suv")   -> 57   (metricKey is the search term itself)
 *
 * Kept updated incrementally by the Kafka listeners in the kafka package —
 * viewReports just reads this table, no fan-out calls at request time.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "report_metric",
        uniqueConstraints = @UniqueConstraint(columnNames = {"report_type", "metric_key"})
)
public class ReportMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private ReportType reportType;

    @Column(name = "metric_key", nullable = false)
    private String metricKey;

    @Column(name = "metric_value", nullable = false)
    private Double metricValue;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}
