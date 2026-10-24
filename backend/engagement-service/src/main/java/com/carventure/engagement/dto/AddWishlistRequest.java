package com.carventure.engagement.dto;

import jakarta.validation.constraints.NotBlank;

public class AddWishlistRequest {

    @NotBlank
    private String customerId;

    @NotBlank
    private String carId;

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }
}
