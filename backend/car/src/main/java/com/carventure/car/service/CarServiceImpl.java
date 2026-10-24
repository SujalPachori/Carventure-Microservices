package com.carventure.car.service;

import com.carventure.car.carkafka.event.CarListedEvent;
import com.carventure.car.carkafka.kafka.CarEventProducer;
import com.carventure.car.client.UserServiceClient;
import com.carventure.car.dto.CarDTO;
import com.carventure.car.dto.CarDTOSched;
import com.carventure.car.dto.CarResponse;
import com.carventure.car.dto.UserResponse;
import com.carventure.car.exception.CarIdNotFoundException;
import com.carventure.car.exception.UserNotFoundException;
import com.carventure.car.model.CarImage;
import com.carventure.car.model.CarSearchFilter;
import com.carventure.car.model.Car;
import com.carventure.car.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.cache.spi.support.DirectAccessRegionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CarServiceImpl implements CarService {

    private final CarRepository carRepository;
    private final UserServiceClient userServiceClient;
    private final FileStorageService storageService;
    private final CarEventProducer carEventProducer;

    // ---- Inventory ----

    @Override
    public CarDTO addCar(CarDTO carDTO) throws UserNotFoundException {
        UserResponse seller = userServiceClient.getSeller(carDTO.getSellerUsername());
        if (seller == null) {
            throw new UserNotFoundException("Seller with username " + carDTO.getSellerUsername() + " was not found");
        }
        Car car = carDTO.toCarEntity();
        car.setCarId(null); // never trust a client-supplied id on create
        car.setSellerId(seller.getUserId()); // ← add this line
        car.setPostedDate(LocalDateTime.now());
        if (car.getStatus() == null) {
            car.setStatus("Draft");
        }

        Car saved = carRepository.save(car);
        carEventProducer.publishCarListed(new CarListedEvent(
                saved.getCarId(),
                saved.getSellerUsername(),
                saved.getBrand(),
                saved.getModel(),
                saved.getPrice(),
                saved.getStatus(),
                saved.getPostedDate()
        ));
        return CarDTO.toCarDTO(saved);
    }

    @Override
    public List<Car> viewAllCars() {
        List<Car> cars = carRepository.findAll();
//        List<CarDTO> carDTOs = new ArrayList<>();
//        for (Car car : cars) {
//            carDTOs.add(CarDTO.toCarDTO(car));
//        }
        return cars;
    }

    @Override
    public List<CarDTO> viewAllCarsBySellerId(String sellerUsername) {
        List<Car> cars = carRepository.findBySellerUsername(sellerUsername);
        List<CarDTO> carDTOs = new ArrayList<>();
        for (Car car : cars) {
            carDTOs.add(CarDTO.toCarDTO(car));
        }
        return carDTOs;
    }

    @Override
    public CarDTO viewCar(Long carId) {
        Car car = findCarOrThrow(carId);
        return CarDTO.toCarDTO(car);
    }

    @Override
    public CarDTO updateCar(Long carId, CarDTO carDTO) {
        Car existing = findCarOrThrow(carId);
        existing.setSellerId(carDTO.getSellerId());
        existing.setBrand(carDTO.getBrand());
        existing.setModel(carDTO.getModel());
        existing.setManufactureYear(carDTO.getManufactureYear());
        existing.setMileage(carDTO.getMileage());
        existing.setFuelType(carDTO.getFuelType());
        existing.setTransmission(carDTO.getTransmission());
        existing.setPrice(carDTO.getPrice());
        existing.setSuggestedPrice(carDTO.getSuggestedPrice());
        existing.setCondition(carDTO.getCondition());
        existing.setDescription(carDTO.getDescription());
        // sellerUsername, postedDate, status, images are intentionally left alone here —
        // status has its own endpoint, images their own upload flow

        Car saved = carRepository.save(existing);
        return CarDTO.toCarDTO(saved);
    }

    @Override
    public void updateCarStatus(Long carId, String status) {
        Car existing = findCarOrThrow(carId);
        existing.setStatus(status);
        Car saved = carRepository.save(existing);
        return;
    }

    @Override
    public void uploadCarDocuments(Long carId, MultipartFile rc, MultipartFile insurance, MultipartFile puc) {
        Car car = findCarOrThrow(carId); // confirms the car exists before accepting documents

        try {
            // 1. Save the file and get its unique saved filename
            String fileName;
            String fileDownloadUrl;
            if(rc!=null) {
                fileName = storageService.saveFile(rc);

                // 2. Build a downloadable URL targeting your download endpoint
                fileDownloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/download/")
                        .path(fileName)
                        .toUriString();

                System.out.println(fileDownloadUrl);
                // 3. Save fileDownloadUrl to your database here (e.g., repository.save(...))
                car.setRcUrl(fileDownloadUrl);

                System.out.println("RC saved successfully");
            }


            if(insurance!=null) {
                fileName = storageService.saveFile(insurance);

                // 2. Build a downloadable URL targeting your download endpoint
                fileDownloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/download/")
                        .path(fileName)
                        .toUriString();

                System.out.println(fileDownloadUrl);
                // 3. Save fileDownloadUrl to your database here (e.g., repository.save(...))
                car.setInsuranceUrl(fileDownloadUrl);
                System.out.println("Insurance saved Successfully");
            }

            if(puc!=null) {

                fileName = storageService.saveFile(puc);

                // 2. Build a downloadable URL targeting your download endpoint
                fileDownloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/download/")
                        .path(fileName)
                        .toUriString();

                System.out.println(fileDownloadUrl);
                // 3. Save fileDownloadUrl to your database here (e.g., repository.save(...))
                car.setPucUrl(fileDownloadUrl);
                System.out.println("PUC saved");
            }


            //return ResponseEntity.ok("File uploaded successfully. URL: " + fileDownloadUrl);
            System.out.println(carRepository.save(car));
        } catch (Exception e) {
            System.out.println(e);
            //return ResponseEntity.status(500).body("Could not upload file: " + e.getMessage());
        }


        // NOTE: there's no CarDocument entity/table yet to persist rc/insurance/puc
        // metadata (file url, verification status, uploaded date) against. This
        // method currently only validates the car exists; wire up storage (e.g. an
        // S3 upload helper) and a CarDocument entity once that's defined, the same
        // way CarImage backs the images list.
//        throw new UnsupportedOperationException(
//                "Document storage not wired up yet — needs a CarDocument entity + S3 upload helper");
    }

    @Override
    public void deleteCar(Long carId) {
        Car existing = findCarOrThrow(carId);
        carRepository.delete(existing);
    }

    // ---- Discovery ----

    @Override
    public List<CarDTO> searchCars(CarSearchFilter filter) {
        List<Car> cars = carRepository.searchCars(
                filter.getBrand(),
                filter.getModel(),
                filter.getMinPrice(),
                filter.getMaxPrice(),
                filter.getYear() != null ? java.time.Year.of(filter.getYear()) : null,
                filter.getMaxMileage(),
                filter.getFuelType(),
                filter.getTransmission(),
                filter.getLocation()
        );
        // filter.getLocation() is intentionally not applied — no location column
        // on Car yet, see the note on CarSearchFilter

        List<CarDTO> carDTOs = new ArrayList<>();
        for (Car car : cars) {
            carDTOs.add(CarDTO.toCarDTO(car));
        }
        return carDTOs;
    }

    @Override
    public Double suggestPrice(Long carId) {
        Car car = findCarOrThrow(carId);
        List<Car> comparables = carRepository.findComparableListings(car.getBrand(), car.getModel());

        double total = 0.0;
        int count = 0;
        for (Car comparable : comparables) {
            if (comparable.getCarId().equals(carId)) {
                continue; // don't compare the car against itself
            }
            if (comparable.getPrice() != null) {
                total += comparable.getPrice();
                count++;
            }
        }

        if (count == 0) {
            return car.getPrice(); // no comparables yet — fall back to the listed price
        }
        return total / count;
    }

    @Override
    public List<CarDTO> getRecommendations(String customerId) {
        // Placeholder heuristic (latest active listings) until a real
        // recommendation engine that factors in customerId's browsing/purchase
        // history exists.
        List<Car> cars = carRepository.findTop10ByStatusOrderByPostedDateDesc("Active");
        List<CarDTO> carDTOs = new ArrayList<>();
        for (Car car : cars) {
            carDTOs.add(CarDTO.toCarDTO(car));
        }
        return carDTOs;
    }

    // ---- helpers ----

    private Car findCarOrThrow(Long carId) {
        Optional<Car> car = carRepository.findById(carId);
        if (car.isEmpty()) {
            throw new CarIdNotFoundException("Car not found with id: " + carId);
        }
        return car.get();
    }

    @Override
    public void uploadCarPictures(Long carId, MultipartFile img) {
        Car car = findCarOrThrow(carId); // confirms the car exists before accepting documents

        try {
            String fileName = storageService.saveFile(img);
            CarImage carImage = new CarImage();
            carImage.setCar(car);
            carImage.setImageUrl(fileName);
            car.getImages().add(carImage);

            carRepository.save(car);
            System.out.println("New Image Added saved successfully");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @Override
    public void deleteCarPicture(Long carId, Long imgId) {
        Car car = findCarOrThrow(carId);
        List<CarImage> carImages = car.getImages();
        carImages.removeIf( carImage -> carImage.getId() == imgId);
        car.setImages(carImages);
        carRepository.save(car);
        System.out.println("Image Deleted successfully");
    }

    @Override
    public CarResponse getCarForPayments(Long carId) {
        Car car = findCarOrThrow(carId);
        if (car.getSellerId() == null) {
            UserResponse seller = userServiceClient.getSeller(car.getSellerUsername());
            if (seller == null) {
                throw new UserNotFoundException("Seller " + car.getSellerUsername() + " not found in User Management");
            }
            car.setSellerId(seller.getUserId());
            car = carRepository.save(car);
        }
        CarResponse carResponse = new CarResponse();
        carResponse.setId(car.getCarId());
        String status = car.getStatus();
        carResponse.setListed(status == null || !("SOLD".equalsIgnoreCase(status) || "RESERVED".equalsIgnoreCase(status)));
        carResponse.setSellerId(Long.valueOf(car.getSellerId()));
        carResponse.setPrice(new BigDecimal(car.getPrice()));
        carResponse.setCurrency("USD");
        return carResponse;
    }
    @Override
    public CarDTOSched getCarForScheduling(Long carId) {
        Car car = findCarOrThrow(carId);
        CarDTOSched carResponse = new CarDTOSched(car.getCarId(), car.getSellerUsername(), car.getModel(), true);
        return carResponse;
    }

}