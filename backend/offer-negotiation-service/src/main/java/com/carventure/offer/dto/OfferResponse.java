package com.carventure.offer.dto;

import com.carventure.offer.entity.Offer;

import java.time.LocalDateTime;

public class OfferResponse {

    private String offerId;
    private String carId;
    private String sellerId;
    private String customerId;
    private Double offeredAmount;
    private String offeredBy;
    private String status;
    private LocalDateTime createdAt;

    private String carBrand;
    private String carModel;
    private Double carPrice;
    private String carImageUrl;
    private String customerName;

    public static OfferResponse from(Offer offer) {
        OfferResponse response = new OfferResponse();
        response.offerId = offer.getOfferId();
        response.carId = offer.getCarId();
        response.sellerId = offer.getSellerId();
        response.customerId = offer.getCustomerId();
        response.offeredAmount = offer.getOfferedAmount();
        response.offeredBy = offer.getOfferedBy();
        response.status = offer.getStatus();
        response.createdAt = offer.getCreatedAt();
        return response;
    }

    public String getOfferId() {
        return offerId;
    }

    public String getCarId() {
        return carId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Double getOfferedAmount() {
        return offeredAmount;
    }

    public String getOfferedBy() {
        return offeredBy;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCarBrand() {
        return carBrand;
    }

    public void setCarBrand(String carBrand) {
        this.carBrand = carBrand;
    }

    public String getCarModel() {
        return carModel;
    }

    public void setCarModel(String carModel) {
        this.carModel = carModel;
    }

    public Double getCarPrice() {
        return carPrice;
    }

    public void setCarPrice(Double carPrice) {
        this.carPrice = carPrice;
    }

    public String getCarImageUrl() {
        return carImageUrl;
    }

    public void setCarImageUrl(String carImageUrl) {
        this.carImageUrl = carImageUrl;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
}
