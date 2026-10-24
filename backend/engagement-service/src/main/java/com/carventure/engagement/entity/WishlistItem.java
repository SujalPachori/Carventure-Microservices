package com.carventure.engagement.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "wishlist_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_wishlist_customer_car",
                columnNames = {"customerId", "carId"}),
        indexes = @Index(name = "idx_wishlist_customer", columnList = "customerId"))
public class WishlistItem {

    @Id
    private String wishlistItemId;

    private String customerId;
    private String carId;
    private LocalDateTime addedAt;

    public WishlistItem() {
    }

    public String getWishlistItemId() {
        return wishlistItemId;
    }

    public void setWishlistItemId(String wishlistItemId) {
        this.wishlistItemId = wishlistItemId;
    }

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

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}
