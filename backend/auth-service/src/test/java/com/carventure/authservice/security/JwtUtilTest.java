package com.carventure.authservice.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String TEST_SECRET =
            "MzJfYnl0ZV9taW5pbXVtX3NlY3JldF9mb3JfaHM1MTJfand0X3NpZ25pbmdfa2V5X2NoYW5nZV9tZQ==";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpirationMs", 900000L);
    }

    @Test
    void generateAccessToken_producesTokenWithCorrectUsernameAndRoles() {
        String token = jwtUtil.generateAccessToken("jane.doe", Set.of("CUSTOMER", "SELLER"));

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("jane.doe");
        assertThat(jwtUtil.extractRoles(token)).containsExactlyInAnyOrder("CUSTOMER", "SELLER");
    }

    @Test
    void generateAccessToken_supportsSingleRole() {
        String token = jwtUtil.generateAccessToken("admin.smith", Set.of("ADMIN"));

        assertThat(jwtUtil.extractRoles(token)).containsExactly("ADMIN");
    }

    @Test
    void isTokenValid_returnsTrue_forFreshTokenAndMatchingUsername() {
        String token = jwtUtil.generateAccessToken("jane.doe", Set.of("CUSTOMER"));

        assertThat(jwtUtil.isTokenValid(token, "jane.doe")).isTrue();
    }

    @Test
    void isTokenValid_returnsFalse_whenUsernameDoesNotMatch() {
        String token = jwtUtil.generateAccessToken("jane.doe", Set.of("CUSTOMER"));

        assertThat(jwtUtil.isTokenValid(token, "someone.else")).isFalse();
    }

    @Test
    void isTokenExpired_returnsFalse_forFreshToken() {
        String token = jwtUtil.generateAccessToken("jane.doe", Set.of("CUSTOMER"));

        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
    }

    @Test
    void extractingClaims_throwsExpiredJwtException_forExpiredToken() {
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpirationMs", -1000L);
        String token = jwtUtil.generateAccessToken("jane.doe", Set.of("CUSTOMER"));

        assertThatThrownBy(() -> jwtUtil.isTokenExpired(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rolesClaimIsInformationalOnly_notUsedForAuthorizationByThisClass() {
        String token = jwtUtil.generateAccessToken("jane.doe", Set.of("CUSTOMER"));

        List<String> roles = jwtUtil.extractRoles(token);

        assertThat(roles).containsExactly("CUSTOMER");
    }
}
