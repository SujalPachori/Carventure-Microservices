package com.carventure.car.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "car_images")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CarImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "car_id")
    private Car car;

    private String imageUrl; // pointing to S3/blob storage — don't store binary in the DB
}