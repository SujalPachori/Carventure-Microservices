package com.carventure.car.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "car")
@NoArgsConstructor
@AllArgsConstructor
public class Car {

    @Id
    @Column(name = "car_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long carId;

    @Column(name = "seller_username", nullable = false)
    private String sellerUsername;

    @Column(name = "seller_id")
    private Integer sellerId;

    private String brand;

    private String model;

    @Column(name = "manufacture_year")
    private Year manufactureYear;

    private Integer mileage;

    @Column(name = "fuel_type")
    private FuelType fuelType;

    private String transmission;

    private String colour;

    private Double price;

    @Column(name = "suggested_price")
    private Double suggestedPrice;

    private String condition;

    private String description;

    @Column(name = "posted_date")
    private LocalDateTime postedDate;

    private String status;

    private String location;

    private String kmdriven;

    private String pincode;

    private String contact;

    private String rcUrl;

    private String insuranceUrl;

    private String pucUrl;

    // Car.java — INVERSE side (mirror only, no FK column on the `cars` table)
    @OneToMany(mappedBy = "car", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CarImage> images;

}
