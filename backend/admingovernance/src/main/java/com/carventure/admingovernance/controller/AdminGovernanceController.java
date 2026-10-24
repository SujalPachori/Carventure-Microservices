package com.carventure.admingovernance.controller;

import com.carventure.admingovernance.dto.DisputeDTO;
import com.carventure.admingovernance.dto.ListingVerificationDTO;
import com.carventure.admingovernance.dto.ResolveDisputeRequest;
import com.carventure.admingovernance.dto.VerifyListingRequest;
import com.carventure.admingovernance.service.AdminGovernanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/governance")
public class AdminGovernanceController {

    private final AdminGovernanceService adminGovernanceService;

    @PutMapping("/verifyListing/{carId}")
    public ResponseEntity<ListingVerificationDTO> verifyListing(
            @PathVariable Long carId,
            @RequestBody VerifyListingRequest request) {
        return ResponseEntity.ok(adminGovernanceService.verifyListing(carId, request));
    }

    @PutMapping("/resolveDispute/{disputeId}")
    public ResponseEntity<DisputeDTO> resolveDispute(
            @PathVariable Long disputeId,
            @RequestBody ResolveDisputeRequest request) {
        return ResponseEntity.ok(adminGovernanceService.resolveDispute(disputeId, request));
    }
}