package com.carventure.authservice.dto;

import com.carventure.authservice.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload for an admin to grant a role to an existing user")
public class RoleAssignmentRequestDTO {

    @NotNull(message = "Role is required")
    @Schema(example = "SELLER")
    private Role role;
}
