package com.carventure.car.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarResponse {
    private Long id;
    private Long sellerId;
    private BigDecimal price;
    private boolean listed;
    private String currency;
}
