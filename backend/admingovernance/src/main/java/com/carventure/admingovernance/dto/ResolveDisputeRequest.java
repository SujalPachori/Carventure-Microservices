package com.carventure.admingovernance.dto;

import com.carventure.admingovernance.model.DisputeStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolveDisputeRequest {
    private DisputeStatus status;
    private String resolutionNotes;
    private String resolvedByAdminUsername;
}