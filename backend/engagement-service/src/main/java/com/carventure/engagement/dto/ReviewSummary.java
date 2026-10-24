package com.carventure.engagement.dto;

public class ReviewSummary {

    private String sellerId;
    private double averageRating;
    private long reviewCount;

    public ReviewSummary(String sellerId, double averageRating, long reviewCount) {
        this.sellerId = sellerId;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }

    public String getSellerId() {
        return sellerId;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public long getReviewCount() {
        return reviewCount;
    }
}
