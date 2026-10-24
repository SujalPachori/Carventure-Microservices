package com.carventure.offer.dto;

import jakarta.validation.constraints.NotBlank;

public class RespondOfferRequest {

    @NotBlank
    private String actorId;

    private Double amount;

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
