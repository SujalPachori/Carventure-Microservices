package com.carventure.admingovernance.dto;

import com.carventure.admingovernance.model.ListingVerification;
import com.carventure.admingovernance.model.VerificationDecision;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListingVerificationDTO {

    private Long id;
    private Long carId;
    private VerificationDecision decision;
    private String reason;
    private String verifiedByAdminUsername;
    private LocalDateTime verifiedDate;

    public ListingVerification toListingVerificationEntity() {
        ListingVerification entity = new ListingVerification();
        entity.setId(this.id);
        entity.setCarId(this.carId);
        entity.setDecision(this.decision);
        entity.setReason(this.reason);
        entity.setVerifiedByAdminUsername(this.verifiedByAdminUsername);
        entity.setVerifiedDate(this.verifiedDate);
        return entity;
    }

    public static ListingVerificationDTO toListingVerificationDTO(ListingVerification entity) {
        if (entity == null) {
            return null;
        }
        ListingVerificationDTO dto = new ListingVerificationDTO();
        dto.setId(entity.getId());
        dto.setCarId(entity.getCarId());
        dto.setDecision(entity.getDecision());
        dto.setReason(entity.getReason());
        dto.setVerifiedByAdminUsername(entity.getVerifiedByAdminUsername());
        dto.setVerifiedDate(entity.getVerifiedDate());
        return dto;
    }
}