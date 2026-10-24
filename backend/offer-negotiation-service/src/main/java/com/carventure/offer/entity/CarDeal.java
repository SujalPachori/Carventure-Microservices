package com.carventure.offer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "car_deals")
public class CarDeal {

    @Id
    private String carId;

    private String acceptedOfferId;
    private String acceptedBuyerId;
    private String sellerId;
    private Double agreedAmount;
    private LocalDateTime acceptedAt;

    public CarDeal() {
    }

    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    public String getAcceptedOfferId() {
        return acceptedOfferId;
    }

    public void setAcceptedOfferId(String acceptedOfferId) {
        this.acceptedOfferId = acceptedOfferId;
    }

    public String getAcceptedBuyerId() {
        return acceptedBuyerId;
    }

    public void setAcceptedBuyerId(String acceptedBuyerId) {
        this.acceptedBuyerId = acceptedBuyerId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public Double getAgreedAmount() {
        return agreedAmount;
    }

    public void setAgreedAmount(Double agreedAmount) {
        this.agreedAmount = agreedAmount;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }
}
