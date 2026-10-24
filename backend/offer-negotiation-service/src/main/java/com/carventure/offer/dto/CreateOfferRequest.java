package com.carventure.offer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateOfferRequest {

    @NotBlank
    private String carId;

    @NotBlank
    private String customerId;

    @NotNull
    @Positive
    private Double offeredAmount;

    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public Double getOfferedAmount() {
        return offeredAmount;
    }

    public void setOfferedAmount(Double offeredAmount) {
        this.offeredAmount = offeredAmount;
    }
}
