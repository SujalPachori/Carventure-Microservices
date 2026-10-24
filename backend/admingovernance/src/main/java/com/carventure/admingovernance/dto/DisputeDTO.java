package com.carventure.admingovernance.dto;

import com.carventure.admingovernance.model.Dispute;
import com.carventure.admingovernance.model.DisputeStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisputeDTO {

    private Long disputeId;
    private Long carId;
    private String buyerUsername;
    private String sellerUsername;
    private DisputeStatus status;
    private String resolutionNotes;
    private String resolvedByAdminUsername;
    private LocalDateTime createdDate;
    private LocalDateTime resolvedDate;

    public Dispute toDisputeEntity() {
        Dispute dispute = new Dispute();
        dispute.setDisputeId(this.disputeId);
        dispute.setCarId(this.carId);
        dispute.setBuyerUsername(this.buyerUsername);
        dispute.setSellerUsername(this.sellerUsername);
        dispute.setStatus(this.status);
        dispute.setResolutionNotes(this.resolutionNotes);
        dispute.setResolvedByAdminUsername(this.resolvedByAdminUsername);
        dispute.setCreatedDate(this.createdDate);
        dispute.setResolvedDate(this.resolvedDate);
        return dispute;
    }

    public static DisputeDTO toDisputeDTO(Dispute dispute) {
        if (dispute == null) {
            return null;
        }
        DisputeDTO dto = new DisputeDTO();
        dto.setDisputeId(dispute.getDisputeId());
        dto.setCarId(dispute.getCarId());
        dto.setBuyerUsername(dispute.getBuyerUsername());
        dto.setSellerUsername(dispute.getSellerUsername());
        dto.setStatus(dispute.getStatus());
        dto.setResolutionNotes(dispute.getResolutionNotes());
        dto.setResolvedByAdminUsername(dispute.getResolvedByAdminUsername());
        dto.setCreatedDate(dispute.getCreatedDate());
        dto.setResolvedDate(dispute.getResolvedDate());
        return dto;
    }
}