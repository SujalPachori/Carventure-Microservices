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
@Schema(description = "A user's current set of roles")
public class UserRolesResponseDTO {

    @Schema(example = "jane.doe")
    private String username;

    @Schema(example = "[\"CUSTOMER\", \"SELLER\"]")
    private Set<String> roles;
}
