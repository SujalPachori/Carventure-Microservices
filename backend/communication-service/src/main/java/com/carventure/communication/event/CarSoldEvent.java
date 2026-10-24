package com.carventure.communication.event;

import java.math.BigDecimal;

public class CarSoldEvent {
    private Long orderId;
    private Long carId;
    private Long sellerId;
    private Long buyerId;
    private BigDecimal amount;
    private String currency;
    private String soldAt;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getCarId() {
        return carId;
    }

    public void setCarId(Long carId) {
        this.carId = carId;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSoldAt() {
        return soldAt;
    }

    public void setSoldAt(String soldAt) {
        this.soldAt = soldAt;
    }
}