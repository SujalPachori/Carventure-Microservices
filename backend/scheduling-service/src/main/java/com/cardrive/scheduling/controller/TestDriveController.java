package com.cardrive.scheduling.controller;

import com.cardrive.scheduling.dto.*;
import com.cardrive.scheduling.exception.ErrorResponse;
import com.cardrive.scheduling.security.CurrentUserProvider;
import com.cardrive.scheduling.service.TestDriveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/test-drives")
@RequiredArgsConstructor
@Tag(name = "Test Drives", description = "Booking lifecycle: request, confirm, reject, cancel, reschedule")
@SecurityRequirement(name = "bearerAuth")
public class TestDriveController {

    private final TestDriveService testDriveService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @Operation(summary = "Request a test drive",
            description = "Buyer picks a car and an arbitrary start time; endTime is derived from the " +
                    "car's configured duration. Validated against seller availability and existing " +
                    "bookings; the database's exclusion constraint is the final guarantee against overlaps.")
    @ApiResponse(responseCode = "201", description = "Test drive created as PENDING")
    @ApiResponse(responseCode = "409", description = "Overlaps an existing PENDING/CONFIRMED booking for this car",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TestDriveResponse> create(@Valid @RequestBody CreateTestDriveRequest request) {
        var response = testDriveService.create(currentUserProvider.getCurrentUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Seller confirms a pending request (PENDING -> CONFIRMED)")
    @ApiResponse(responseCode = "200", description = "Confirmed")
    @ApiResponse(responseCode = "403", description = "Caller does not own the car")
    @ApiResponse(responseCode = "409", description = "Test drive is not in PENDING status")
    public ResponseEntity<TestDriveResponse> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(testDriveService.confirm(currentUserProvider.getCurrentUser(), id));
    }

    @PostMapping(value = "/{id}/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Seller rejects a pending request (PENDING -> REJECTED)")
    public ResponseEntity<TestDriveResponse> reject(@PathVariable Long id,
                                                      @RequestBody(required = false) RejectRequest request) {
        String reason = request != null ? request.reason() : null;
        return ResponseEntity.ok(testDriveService.reject(currentUserProvider.getCurrentUser(), id, reason));
    }

    @PostMapping(value = "/{id}/cancel", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Buyer or seller cancels a PENDING or CONFIRMED booking")
    public ResponseEntity<TestDriveResponse> cancel(@PathVariable Long id,
                                                      @RequestBody(required = false) CancelRequest request) {
        String reason = request != null ? request.reason() : null;
        return ResponseEntity.ok(testDriveService.cancel(currentUserProvider.getCurrentUser(), id, reason));
    }

    @PostMapping("/{id}/reschedule")
    @Operation(summary = "Atomically move a booking to a new time",
            description = "Re-runs all validation (future time, availability, conflicts) and updates the " +
                    "existing row in place - never cancel-then-recreate. Resets status to PENDING.")
    public ResponseEntity<TestDriveResponse> reschedule(@PathVariable Long id,
                                                          @Valid @RequestBody RescheduleRequest request) {
        return ResponseEntity.ok(testDriveService.reschedule(currentUserProvider.getCurrentUser(), id, request));
    }

    @GetMapping("/mine")
    @Operation(summary = "List the caller's own test drives (buyer: requested; seller: on their cars)")
    public ResponseEntity<List<TestDriveResponse>> getMine() {
        return ResponseEntity.ok(testDriveService.getMine(currentUserProvider.getCurrentUser()));
    }

    @GetMapping
    @Operation(summary = "Admin: list every test drive booking in the system")
    public ResponseEntity<List<TestDriveResponse>> getAllForAdmin() {
        return ResponseEntity.ok(testDriveService.getAllForAdmin(currentUserProvider.getCurrentUser()));
    }
}
