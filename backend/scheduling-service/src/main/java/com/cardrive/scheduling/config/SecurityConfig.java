package com.cardrive.scheduling.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

/**
 * JWT resource-server security config, wired to match auth-service's
 * com.carventure.authservice.security.JwtUtil exactly:
 *
 *   - auth-service signs with a SYMMETRIC HMAC key via
 *     Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)) - there is no JWKS
 *     endpoint or issuer discovery document to point at, so this service
 *     verifies with the SAME base64 secret (jwt.secret must be byte-for-byte
 *     identical to auth-service's), not RS256/JWKS.
 *   - jjwt 0.12.x's signWith(Key) with no explicit algorithm auto-selects the
 *     strongest HMAC algorithm the key length supports. auth-service's
 *     DEFAULT secret is 58 bytes (464 bits) -> satisfies HS384's 48-byte
 *     minimum but not HS512's 64-byte minimum, so it signs with HS384. This
 *     is secret-length-dependent: if auth-service's deployed secret is ever
 *     changed to >=64 bytes, it will silently switch to signing with HS512
 *     and this service's decoder must be updated (jwt.mac-algorithm) to match,
 *     or verification will fail for every token. Flag to whoever owns
 *     auth-service: pinning an explicit algorithm there (rather than relying
 *     on key-length auto-selection) would remove this footgun platform-wide.
 *   - roles come back as a "roles" claim (a JSON array), not a singular "role".
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecretBase64;

    @Value("${jwt.mac-algorithm:HS384}")
    private String macAlgorithmName;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                        .decoder(jwtDecoder())
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );
        return http.build();
    }

    /**
     * Verifies tokens with the same shared secret auth-service signs with -
     * mirrors JwtUtil.signingKey() (Keys.hmacShaKeyFor over the base64-decoded
     * secret) exactly, using Nimbus's HMAC support instead of jjwt.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecretBase64);
        MacAlgorithm macAlgorithm = resolveMacAlgorithm(macAlgorithmName);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, jcaAlgorithmName(macAlgorithmName));
        return NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(macAlgorithm).build();
    }

    private MacAlgorithm resolveMacAlgorithm(String name) {
        return switch (name.toUpperCase()) {
            case "HS256" -> MacAlgorithm.HS256;
            case "HS384" -> MacAlgorithm.HS384;
            case "HS512" -> MacAlgorithm.HS512;
            default -> throw new IllegalStateException("Unsupported jwt.mac-algorithm: " + name
                    + " (must be HS256, HS384, or HS512)");
        };
    }

    /** SecretKeySpec needs the JCA cipher name, not the JWA name ("HS384" vs "HmacSHA384"). */
    private String jcaAlgorithmName(String macAlgorithmName) {
        return switch (macAlgorithmName.toUpperCase()) {
            case "HS256" -> "HmacSHA256";
            case "HS384" -> "HmacSHA384";
            case "HS512" -> "HmacSHA512";
            default -> throw new IllegalStateException("Unsupported jwt.mac-algorithm: " + macAlgorithmName);
        };
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return converter;
    }

    /**
     * auth-service puts roles in a "roles" claim - a JSON array - not a
     * singular "role" string. A caller can legitimately carry more than one
     * (e.g. ["CUSTOMER","SELLER"]).
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .toList();
    }
}
