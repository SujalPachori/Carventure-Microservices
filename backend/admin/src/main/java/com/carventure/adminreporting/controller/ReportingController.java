package com.carventure.adminreporting.controller;


import com.carventure.adminreporting.dto.ReportDTO;
import com.carventure.adminreporting.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/reporting")
public class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/viewReports")
    public ResponseEntity<List<ReportDTO>> viewReports() {
        return ResponseEntity.ok(reportingService.viewReports());
    }
}