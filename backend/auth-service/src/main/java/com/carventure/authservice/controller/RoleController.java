package com.carventure.authservice.controller;

import com.carventure.authservice.dto.RoleAssignmentRequestDTO;
import com.carventure.authservice.dto.UserRolesResponseDTO;
import com.carventure.authservice.entity.Role;
import com.carventure.authservice.exception.ErrorResponse;
import com.carventure.authservice.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Every endpoint here requires an authenticated caller holding ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/auth/admin/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin - Role Management", description = "Admin-only endpoints for granting roles to existing users")
public class RoleController {

    private final RoleService roleService;

    @PostMapping("/{username}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Grant a role to an existing user",
            description = "Admin-only. A user may hold CUSTOMER and/or SELLER simultaneously, but ADMIN is "
                    + "always mutually exclusive with both - granting ADMIN to a buyer/seller (or vice versa) is rejected.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role granted",
                    content = @Content(schema = @Schema(implementation = UserRolesResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Role assignment violates the admin/buyer-seller exclusivity rule, "
                    + "or the user already has this role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserRolesResponseDTO> addRole(
            @Parameter(example = "jane.doe") @PathVariable String username,
            @Valid @RequestBody RoleAssignmentRequestDTO request) {
        return ResponseEntity.ok(roleService.addRole(username, request.getRole()));
    }

    @GetMapping("/{username}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "View a user's current roles", description = "Admin-only")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Roles returned",
                    content = @Content(schema = @Schema(implementation = UserRolesResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserRolesResponseDTO> getRoles(@PathVariable String username) {
        return ResponseEntity.ok(roleService.getRoles(username));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List users by role",
            description = "Admin-only. Returns every user currently holding the given role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users returned",
                    content = @Content(schema = @Schema(implementation = UserRolesResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or unknown role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Caller is not an admin",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<String>> getUsersByRole(
            @Parameter(example = "SELLER") @RequestParam Role role) {
        return ResponseEntity.ok(roleService.getUsersByRole(role));
    }
}
