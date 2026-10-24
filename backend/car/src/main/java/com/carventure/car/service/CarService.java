package com.carventure.car.service;

import com.carventure.car.dto.CarDTO;
import com.carventure.car.dto.CarDTOSched;
import com.carventure.car.dto.CarResponse;
import com.carventure.car.model.Car;
import com.carventure.car.model.CarSearchFilter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CarService {

    // ---- Inventory ----

    CarDTO addCar(CarDTO carDTO);

    List<Car> viewAllCars();

    List<CarDTO> viewAllCarsBySellerId(String sellerUsername);

    CarDTO viewCar(Long carId);

    CarDTO updateCar(Long carId, CarDTO carDTO);

    void updateCarStatus(Long carId, String status);

    // rc/insurance/puc — one call, three optional files, each may be null if not being (re)uploaded
    void uploadCarDocuments(Long carId, MultipartFile rc, MultipartFile insurance, MultipartFile puc);

    void deleteCar(Long carId);

    // ---- Discovery ----

    List<CarDTO> searchCars(CarSearchFilter filter);

    Double suggestPrice(Long carId);

    List<CarDTO> getRecommendations(String customerId);

    void uploadCarPictures(Long carId, MultipartFile img);
    void deleteCarPicture(Long carId, Long imgId);

    public CarResponse getCarForPayments(Long carId);
    public CarDTOSched getCarForScheduling(Long carId);
    //public CarDTOSched updateCarAfterScheduling(CarDTOSched carDTOSched);
}