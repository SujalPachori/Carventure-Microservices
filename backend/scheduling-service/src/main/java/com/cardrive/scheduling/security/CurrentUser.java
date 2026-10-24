package com.cardrive.scheduling.security;

import lombok.Value;

import java.util.Set;

/**
 * Represents the authenticated caller, resolved from the JWT security context.
 * Never trust an equivalent payload supplied by the client body.
 *
 * Identity is the username (auth-service's User primary key is the username
 * itself - there is no numeric user id anywhere in the platform). roles is a
 * SET because auth-service allows a user to hold CUSTOMER and SELLER at the
 * same time; ADMIN is always mutually exclusive with both, per auth-service's
 * own invariant, but this service doesn't re-enforce that - it just checks
 * membership.
 */
@Value
public class CurrentUser {
    String username;
    Set<Role> roles;

    public boolean isBuyer() {
        return roles.contains(Role.CUSTOMER);
    }

    public boolean isSeller() {
        return roles.contains(Role.SELLER);
    }

    public boolean isAdmin() {
        return roles.contains(Role.ADMIN);
    }
}
