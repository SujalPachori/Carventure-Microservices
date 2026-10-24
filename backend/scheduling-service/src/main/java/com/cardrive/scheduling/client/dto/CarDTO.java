package com.cardrive.scheduling.client.dto;

/**
 * Contract with the separate Car Discovery Service. sellerUsername (not a
 * numeric sellerId) - platform identity is username-keyed end to end
 * (auth-service's User is keyed by username; there is no numeric user id
 * anywhere), so Car Discovery Service is assumed to expose car ownership the
 * same way. If it currently exposes a numeric sellerId instead, that's the
 * next thing to align - flag it to whoever owns that service.
 */
public record CarDTO(
        Long id,
        String sellerUsername,
        String name,
        boolean active
) {
}
