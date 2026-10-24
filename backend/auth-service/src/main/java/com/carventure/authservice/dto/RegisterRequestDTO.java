package com.carventure.authservice.dto;

import com.carventure.authservice.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload to self-register a new account. Always assigned the CUSTOMER role - "
        + "there is no way to request SELLER or ADMIN here.")
public class RegisterRequestDTO {

    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 100, message = "Username must be between 4 and 100 characters")
    @Schema(example = "jane.doe")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    @Schema(example = "S3curePass!23")
    private String password;

    @NotNull(message = "Role is required")
    @Schema(example = "CUSTOMER", description = "Must be CUSTOMER or SELLER. ADMIN is not allowed at self-registration.")
    private Role role;

    @NotBlank(message = "First Name cannot be blank")
    private String firstName;

    @NotBlank(message = "First Name cannot be blank")
    private String lastName;

    @NotBlank(message = "First Name cannot be blank")
    @Email
    private String email;

    @NotBlank(message = "First Name cannot be blank")
    private String phoneNumber;
    @NotBlank
    private String address;
    @NotBlank
    private String pincode;
    private LocalDate dateOfBirth;
}
