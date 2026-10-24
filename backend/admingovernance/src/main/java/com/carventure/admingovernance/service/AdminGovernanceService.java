package com.carventure.admingovernance.service;

import com.carventure.admingovernance.dto.DisputeDTO;
import com.carventure.admingovernance.dto.ListingVerificationDTO;
import com.carventure.admingovernance.dto.ResolveDisputeRequest;
import com.carventure.admingovernance.dto.VerifyListingRequest;

public interface AdminGovernanceService {

    public ListingVerificationDTO verifyListing(Long carId, VerifyListingRequest request);

    public DisputeDTO resolveDispute(Long disputeId, ResolveDisputeRequest request);
}