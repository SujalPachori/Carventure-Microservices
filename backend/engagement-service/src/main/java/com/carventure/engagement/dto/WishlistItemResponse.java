package com.carventure.engagement.dto;

import com.carventure.engagement.entity.WishlistItem;

import java.time.LocalDateTime;

public class WishlistItemResponse {

    private String wishlistItemId;
    private String carId;
    private LocalDateTime addedAt;

    private String brand;
    private String model;
    private Integer manufactureYear;
    private Double price;
    private String imageUrl;
    private String carStatus;
    private boolean available;

    public static WishlistItemResponse from(WishlistItem item) {
        WishlistItemResponse response = new WishlistItemResponse();
        response.wishlistItemId = item.getWishlistItemId();
        response.carId = item.getCarId();
        response.addedAt = item.getAddedAt();
        response.available = false;
        return response;
    }

    public String getWishlistItemId() {
        return wishlistItemId;
    }

    public String getCarId() {
        return carId;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getManufactureYear() {
        return manufactureYear;
    }

    public void setManufactureYear(Integer manufactureYear) {
        this.manufactureYear = manufactureYear;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCarStatus() {
        return carStatus;
    }

    public void setCarStatus(String carStatus) {
        this.carStatus = carStatus;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
