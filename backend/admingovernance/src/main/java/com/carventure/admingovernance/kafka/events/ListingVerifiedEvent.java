package com.carventure.admingovernance.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListingVerifiedEvent {
    private Long carId;
    private String decision; // APPROVED, REJECTED, FLAGGED — kept as a String so this event has no dependency on this service's enum type
}