package com.carventure.adminreporting.repository;

import com.carventure.adminreporting.model.ReportMetric;
import com.carventure.adminreporting.model.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportMetricRepository extends JpaRepository<ReportMetric, Long> {

    // used by viewReports
    List<ReportMetric> findByReportType(ReportType reportType);

    Optional<ReportMetric> findByReportTypeAndMetricKey(ReportType reportType, String metricKey);

    // atomic increment at the DB level — avoids the lost-update race of a
    // plain find -> add -> save when two Kafka messages for the same metric
    // land close together. Returns the number of rows updated (0 means the
    // metric doesn't exist yet and the caller should insert it instead).
    @Modifying
    @Query("""
            UPDATE ReportMetric m
            SET m.metricValue = m.metricValue + :amount, m.lastUpdated = :now
            WHERE m.reportType = :reportType AND m.metricKey = :metricKey
            """)
    int incrementIfExists(
            @Param("reportType") ReportType reportType,
            @Param("metricKey") String metricKey,
            @Param("amount") double amount,
            @Param("now") LocalDateTime now
    );
}