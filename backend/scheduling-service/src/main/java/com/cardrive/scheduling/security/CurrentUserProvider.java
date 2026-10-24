package com.cardrive.scheduling.security;

import com.cardrive.scheduling.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves the CurrentUser from the Spring Security context, matching exactly
 * what com.carventure.authservice.security.JwtUtil puts in the token:
 *   - "sub"   : the username (JwtUtil.buildToken sets .subject(username))
 *   - "roles" : a JSON array of role names, e.g. ["CUSTOMER","SELLER"]
 *               (JwtUtil.generateAccessToken: claims.put("roles", List.copyOf(roles)))
 *
 * There is no numeric user id claim - auth-service's User entity is keyed by
 * username, so this service is too.
 */
@Component
public class CurrentUserProvider {

    public CurrentUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new UnauthorizedException("No authenticated principal found");
        }

        String username = jwt.getSubject();
        List<String> roleClaims = jwt.getClaimAsStringList("roles");

        if (username == null || roleClaims == null || roleClaims.isEmpty()) {
            throw new UnauthorizedException("JWT is missing required sub/roles claims");
        }

        Set<Role> roles = EnumSet.noneOf(Role.class);
        for (String r : roleClaims) {
            try {
                roles.add(Role.valueOf(r.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new UnauthorizedException("JWT carries an unrecognized role: " + r);
            }
        }

        return new CurrentUser(username, roles);
    }
}
