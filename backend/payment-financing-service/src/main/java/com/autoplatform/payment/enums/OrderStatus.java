package com.autoplatform.payment.enums;

/**
 * Lifecycle of a car purchase order.
 * PAID and FINANCING_ACTIVE are the only "sold" states - the DB-level
 * uniqueness guarantee (see V1__init.sql) allows at most one order per
 * car to ever reach either of these.
 */
public enum OrderStatus {
    CREATED,
    AWAITING_PAYMENT,
    PAID,
    FINANCING_ACTIVE,
    PAYMENT_FAILED,
    CANCELLED,
    EXPIRED,
    REFUNDED;

    public boolean isSold() {
        return this == PAID || this == FINANCING_ACTIVE;
    }

    public boolean isTerminalOrSold() {
        return isSold() || this == PAYMENT_FAILED || this == CANCELLED || this == REFUNDED || this == EXPIRED;
    }
}
