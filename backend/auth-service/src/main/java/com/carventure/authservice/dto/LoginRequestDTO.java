package com.carventure.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Credentials used to authenticate and obtain a JWT")
public class LoginRequestDTO {

    @NotBlank(message = "Username is required")
    @Schema(example = "jane.doe")
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(example = "S3curePass!23")
    private String password;
}
