package com.cardrive.scheduling.security;

/**
 * Mirrors com.carventure.authservice.entity.Role exactly - CUSTOMER (not
 * BUYER), SELLER, ADMIN. A user may hold CUSTOMER and/or SELLER simultaneously
 * (auth-service's role model); ADMIN is always mutually exclusive with both,
 * enforced on that side, not this one.
 */
public enum Role {
    CUSTOMER,
    SELLER,
    ADMIN
}
