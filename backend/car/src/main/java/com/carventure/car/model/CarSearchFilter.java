package com.carventure.car.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Holds the optional query params accepted by GET /searchCars.
 * Any field left null is treated as "no filter on this attribute".
 *
 * NOTE: `location` is part of the requested filter set but there is currently
 * no location column on the Car entity. Left in here so the controller/service
 * signature doesn't need to change again once that column is added — it's
 * simply ignored by the repository query below until then.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarSearchFilter {
    private String brand;
    private String model;
    private Double minPrice;
    private Double maxPrice;
    private Integer year;
    private Integer maxMileage;
    private FuelType fuelType;
    private String transmission;
    private String location;
}
