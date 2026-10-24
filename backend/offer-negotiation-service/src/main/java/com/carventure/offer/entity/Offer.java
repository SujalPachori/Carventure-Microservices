package com.carventure.offer.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "offers", indexes = {
        @Index(name = "idx_offer_car", columnList = "carId"),
        @Index(name = "idx_offer_customer", columnList = "customerId"),
        @Index(name = "idx_offer_seller", columnList = "sellerId")
})
public class Offer {

    @Id
    private String offerId;

    private String carId;
    private String sellerId;
    private String customerId;

    private Double offeredAmount;
    private String offeredBy;
    private String parentOfferId;
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public Offer() {
    }

    public String getOfferId() {
        return offerId;
    }

    public void setOfferId(String offerId) {
        this.offerId = offerId;
    }

    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
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

    public String getOfferedBy() {
        return offeredBy;
    }

    public void setOfferedBy(String offeredBy) {
        this.offeredBy = offeredBy;
    }

    public String getParentOfferId() {
        return parentOfferId;
    }

    public void setParentOfferId(String parentOfferId) {
        this.parentOfferId = parentOfferId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }
}
