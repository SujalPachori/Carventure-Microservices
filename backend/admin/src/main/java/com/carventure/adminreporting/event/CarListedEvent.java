package com.carventure.adminreporting.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarListedEvent {
    private Long carId;
    private String sellerUsername;
    private String brand;
    private String model;
    private double price;
    private String status;
    private LocalDateTime postedDate;
}
