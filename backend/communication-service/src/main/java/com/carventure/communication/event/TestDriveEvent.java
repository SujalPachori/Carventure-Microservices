package com.carventure.communication.event;

public class TestDriveEvent {

    private Long testDriveId;
    private Long carId;
    private String sellerUsername;
    private String buyerUsername;
    private String startTime;
    private String endTime;
    private String status;

    public void setTestDriveId(Long testDriveId) {
        this.testDriveId = testDriveId;
    }

    public void setCarId(Long carId) {
        this.carId = carId;
    }

    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }

    public void setBuyerUsername(String buyerUsername) {
        this.buyerUsername = buyerUsername;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    private String eventType;

    public Long getTestDriveId() {
        return testDriveId;
    }

    public Long getCarId() {
        return carId;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public String getBuyerUsername() {
        return buyerUsername;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }

    public String getEventType() {
        return eventType;
    }
// getters and setters for all eight fields
}