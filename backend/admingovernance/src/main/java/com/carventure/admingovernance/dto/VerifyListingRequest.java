package com.carventure.admingovernance.dto;

import com.carventure.admingovernance.model.VerificationDecision;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyListingRequest {
    private VerificationDecision decision;
    private String reason;
    private String verifiedByAdminUsername;
}