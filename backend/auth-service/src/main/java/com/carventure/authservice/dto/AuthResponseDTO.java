package com.carventure.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Issued tokens and basic identity info returned after a successful auth operation")
public class AuthResponseDTO {

    @Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(example = "8f14e45f-ceea-4c19-8b1e-6b1a3f2a1c9d")
    private String refreshToken;

    @Builder.Default
    @Schema(example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(example = "jane.doe")
    private String username;

    @Schema(example = "[\"CUSTOMER\"]")
    private Set<String> roles;

    @Schema(description = "Access token expiry in seconds from issue time", example = "900")
    private long expiresInSeconds;
}
