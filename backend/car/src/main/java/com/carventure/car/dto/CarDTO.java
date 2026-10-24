package com.carventure.car.dto;

import com.carventure.car.model.Car;
import com.carventure.car.model.CarImage;
import com.carventure.car.model.FuelType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarDTO {

    private Long carId;

    private String sellerUsername;

    private Integer sellerId;

    private String brand;

    private String model;

    private Year manufactureYear;

    private String colour;

    private Integer mileage;

    private FuelType fuelType;

    private String transmission;

    private Double price;

    private Double suggestedPrice;

    private String condition;

    private String location;

    private String kmdriven;

    private String pincode;

    private String contact;

    private String description;

    private LocalDateTime postedDate;

    private String status;

    // Controller only ever sees plain URLs, never the CarImage entity
    private List<String> imageUrls;

    /**
     * Converts this DTO into a Car entity. Rebuilds the CarImage list and wires
     * each CarImage back to the parent Car (required since Car.images is the
     * inverse/mappedBy side of the relationship).
     */
    public Car toCarEntity() {
        Car car = new Car();
        car.setCarId(this.carId);
        car.setSellerUsername(this.sellerUsername);
        car.setSellerId(this.sellerId);
        car.setBrand(this.brand);
        car.setModel(this.model);
        car.setManufactureYear(this.manufactureYear);
        car.setColour(this.colour);
        car.setMileage(this.mileage);
        car.setFuelType(this.fuelType);
        car.setTransmission(this.transmission);
        car.setPincode(this.pincode);
        car.setContact(this.contact);
        car.setKmdriven(this.kmdriven);
        car.setPrice(this.price);
        car.setSuggestedPrice(this.suggestedPrice);
        car.setCondition(this.condition);
        car.setDescription(this.description);
        car.setPostedDate(this.postedDate);
        car.setStatus(this.status);
        car.setLocation(this.location);

        List<CarImage> carImages = new ArrayList<>();
        if (this.imageUrls != null) {
            int order = 0;
            for (String url : this.imageUrls) {
                CarImage carImage = new CarImage();
                carImage.setImageUrl(url);
                carImage.setCar(car);
                carImages.add(carImage);
                order++;
            }
        }
        car.setImages(carImages);

        return car;
    }

    /**
     * Converts a Car entity into a DTO for the controller layer.
     */
    public static CarDTO toCarDTO(Car car) {
        if (car == null) {
            return null;
        }

        CarDTO carDTO = new CarDTO();
        carDTO.setCarId(car.getCarId());
        carDTO.setSellerUsername(car.getSellerUsername());
        carDTO.setSellerId(car.getSellerId());
        carDTO.setBrand(car.getBrand());
        carDTO.setModel(car.getModel());
        carDTO.setManufactureYear(car.getManufactureYear());
        carDTO.setColour(car.getColour());
        carDTO.setMileage(car.getMileage());
        carDTO.setFuelType(car.getFuelType());
        carDTO.setTransmission(car.getTransmission());
        carDTO.setPrice(car.getPrice());
        carDTO.setSuggestedPrice(car.getSuggestedPrice());
        carDTO.setCondition(car.getCondition());
        carDTO.setDescription(car.getDescription());
        carDTO.setPostedDate(car.getPostedDate());
        carDTO.setStatus(car.getStatus());
        carDTO.setLocation(car.getLocation());
        carDTO.setKmdriven(car.getKmdriven());
        carDTO.setPincode(car.getPincode());
        carDTO.setContact(car.getContact());

        List<String> imageUrls = new ArrayList<>();
        if (car.getImages() != null) {
            for (CarImage carImage : car.getImages()) {
                imageUrls.add(carImage.getImageUrl());
            }
        }
        carDTO.setImageUrls(imageUrls);

        return carDTO;
    }
}
