package com.carventure.authservice.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard error payload returned for all API failures")
public class ErrorResponse {

    @Schema(example = "401")
    private int status;

    @Schema(example = "UNAUTHORIZED")
    private String error;

    @Schema(example = "Invalid username or password")
    private String message;

    @Schema(example = "/api/auth/login")
    private String path;

    @Schema(example = "2026-08-15T10:15:30")
    private LocalDateTime timestamp;

    private Map<String, String> validationErrors;
}
