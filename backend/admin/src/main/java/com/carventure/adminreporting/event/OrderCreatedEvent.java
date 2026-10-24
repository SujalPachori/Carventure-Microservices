package com.carventure.adminreporting.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;
    private Long carId;
    private String buyerUsername;
    private String sellerUsername;
    private double amount;
    private String status;
    private LocalDateTime orderDate;
}
