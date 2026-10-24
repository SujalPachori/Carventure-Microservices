package com.cardrive.scheduling.controller;

import com.cardrive.scheduling.dto.AvailabilityResponse;
import com.cardrive.scheduling.dto.CreateAvailabilityRequest;
import com.cardrive.scheduling.dto.UpdateAvailabilityRequest;
import com.cardrive.scheduling.exception.ErrorResponse;
import com.cardrive.scheduling.security.CurrentUserProvider;
import com.cardrive.scheduling.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Availability", description = "Seller-defined recurring weekly availability per car")
@SecurityRequirement(name = "bearerAuth")
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/availability")
    @Operation(summary = "Create a recurring availability window for a car (seller only)")
    @ApiResponse(responseCode = "201", description = "Availability window created")
    @ApiResponse(responseCode = "403", description = "Caller does not own the car",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<AvailabilityResponse> create(@Valid @RequestBody CreateAvailabilityRequest request) {
        var response = availabilityService.create(currentUserProvider.getCurrentUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/availability/{id}")
    @Operation(summary = "Update an existing availability window (seller only, owner only)")
    public ResponseEntity<AvailabilityResponse> update(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateAvailabilityRequest request) {
        return ResponseEntity.ok(availabilityService.update(currentUserProvider.getCurrentUser(), id, request));
    }

    @DeleteMapping("/availability/{id}")
    @Operation(summary = "Deactivate an availability window (soft delete, seller only, owner only)")
    @ApiResponse(responseCode = "204", description = "Deactivated")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        availabilityService.deactivate(currentUserProvider.getCurrentUser(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cars/{carId}/availability")
    @Operation(summary = "View a car's configured availability windows",
            description = "Open to any authenticated role - buyers need this to pick a valid slot.")
    public ResponseEntity<List<AvailabilityResponse>> getForCar(@PathVariable Long carId) {
        return ResponseEntity.ok(availabilityService.getForCar(carId));
    }
}
