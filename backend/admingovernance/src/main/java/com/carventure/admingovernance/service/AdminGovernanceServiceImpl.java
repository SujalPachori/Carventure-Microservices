package com.carventure.admingovernance.service;
import com.carventure.admingovernance.dto.DisputeDTO;
import com.carventure.admingovernance.dto.ListingVerificationDTO;
import com.carventure.admingovernance.dto.ResolveDisputeRequest;
import com.carventure.admingovernance.dto.VerifyListingRequest;
import com.carventure.admingovernance.client.CarServiceClient;
import com.carventure.admingovernance.kafka.ListingVerificationEventPublisher;
import com.carventure.admingovernance.model.Dispute;
import com.carventure.admingovernance.model.DisputeStatus;
import com.carventure.admingovernance.model.ListingVerification;
import com.carventure.admingovernance.model.VerificationDecision;
import com.carventure.admingovernance.repository.DisputeRepository;
import com.carventure.admingovernance.repository.ListingVerificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminGovernanceServiceImpl implements AdminGovernanceService {

    private final ListingVerificationRepository listingVerificationRepository;
    private final DisputeRepository disputeRepository;
    private final CarServiceClient carServiceClient;
    private final ListingVerificationEventPublisher listingVerificationEventPublisher;

    @Override
    public ListingVerificationDTO verifyListing(Long carId, VerifyListingRequest request) {
        ListingVerification verification = new ListingVerification();
        verification.setCarId(carId);
        verification.setDecision(request.getDecision());
        verification.setReason(request.getReason());
        verification.setVerifiedByAdminUsername(request.getVerifiedByAdminUsername());
        verification.setVerifiedDate(LocalDateTime.now());

        ListingVerification saved = listingVerificationRepository.save(verification);

        // this service owns the decision, but not Car.status (Car service does)
        // or the INSPECTIONS metric (Reporting service does) — both are notified
        carServiceClient.updateCarStatus(carId, mapDecisionToCarStatus(request.getDecision()));
        listingVerificationEventPublisher.publish(carId, request.getDecision().name());

        return ListingVerificationDTO.toListingVerificationDTO(saved);
    }

    @Override
    public DisputeDTO resolveDispute(Long disputeId, ResolveDisputeRequest request) {
        Optional<Dispute> existing = disputeRepository.findById(disputeId);
        if (existing.isEmpty()) {
            throw new EntityNotFoundException("Dispute not found with id: " + disputeId);
        }

        Dispute dispute = existing.get();
        dispute.setStatus(request.getStatus());
        dispute.setResolutionNotes(request.getResolutionNotes());
        dispute.setResolvedByAdminUsername(request.getResolvedByAdminUsername());
        if (request.getStatus() == DisputeStatus.RESOLVED || request.getStatus() == DisputeStatus.REJECTED) {
            dispute.setResolvedDate(LocalDateTime.now());
        }

        Dispute saved = disputeRepository.save(dispute);
        return DisputeDTO.toDisputeDTO(saved);
    }

    private String mapDecisionToCarStatus(VerificationDecision decision) {
        switch (decision) {
            case APPROVED:
                return "Active";
            case REJECTED:
                return "Rejected";
            case FLAGGED:
                return "Flagged";
            default:
                throw new IllegalArgumentException("Unhandled decision: " + decision);
        }
    }
}