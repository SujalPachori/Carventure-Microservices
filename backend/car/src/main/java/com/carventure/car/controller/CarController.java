package com.carventure.car.controller;

import com.carventure.car.dto.CarDTO;
import com.carventure.car.dto.CarDTOSched;
import com.carventure.car.dto.CarResponse;
import com.carventure.car.model.Car;
import com.carventure.car.model.CarSearchFilter;
import com.carventure.car.service.CarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Tag(name = "Car", description = "Endpoints for car inventory management and discovery")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/car")
public class CarController {

    private final CarService carService;

    // ---- Inventory ----

    @Operation(summary = "Add a new car listing", description = "Creates a new car listing owned by the authenticated seller")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Car created successfully",
                    content = @Content(schema = @Schema(implementation = CarDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid car payload", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @PostMapping("/addCar")
    public ResponseEntity<CarDTO> addCar(@RequestBody CarDTO carDTO) {
        CarDTO created = carService.addCar(carDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "View all cars")
    @ApiResponse(responseCode = "200", description = "List of all cars retrieved")
    @GetMapping("/viewAllCars")
    public ResponseEntity<List<Car>> viewAllCars() {
        return ResponseEntity.ok(carService.viewAllCars());
    }

    @Operation(summary = "View cars by seller")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cars for seller retrieved"),
            @ApiResponse(responseCode = "404", description = "Seller not found", content = @Content)
    })
    @GetMapping("/viewAllCarsBySellerId/{sellerId}")
    public ResponseEntity<List<CarDTO>> viewAllCarsBySellerId(@PathVariable String sellerUsername) {
        return ResponseEntity.ok(carService.viewAllCarsBySellerId(sellerUsername));
    }

    @Operation(summary = "Get a single car by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Car found"),
            @ApiResponse(responseCode = "404", description = "Car not found", content = @Content)
    })
    @GetMapping("/viewCar/{carId}")
    public ResponseEntity<CarDTO> viewCar(@PathVariable Long carId) {
        return ResponseEntity.ok(carService.viewCar(carId));
    }

    @PutMapping("/updateCar/{carId}")
    public ResponseEntity<CarDTO> updateCar(@PathVariable Long carId, @RequestBody CarDTO carDTO) {
        return ResponseEntity.ok(carService.updateCar(carId, carDTO));
    }

    @Operation(summary = "Update car status", description = "Updates status (e.g. AVAILABLE, SOLD, PENDING)")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @PutMapping("/updateCarStatus/{carId}")
    public void updateCarStatus(@PathVariable Long carId, @RequestParam String status) {
        carService.updateCarStatus(carId, status);
        return;
    }

    @Operation(summary = "Upload car documents (RC, insurance, PUC)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Documents uploaded"),
            @ApiResponse(responseCode = "400", description = "Invalid file(s)", content = @Content)
    })
    @PostMapping(value = "/uploadCarDocuments/{carId}", consumes = "multipart/form-data")
    public ResponseEntity<Void> uploadCarDocuments(
            @PathVariable Long carId,
            @RequestParam(required = false) MultipartFile rc,
            @RequestParam(required = false) MultipartFile insurance,
            @RequestParam(required = false) MultipartFile puc) {
        carService.uploadCarDocuments(carId, rc, insurance, puc);
        return ResponseEntity.ok().build();
    }


    @Operation(summary = "Delete a car listing")
    @ApiResponse(responseCode = "204", description = "Car deleted")
    @DeleteMapping("/deleteCar/{carId}")
    public ResponseEntity<Void> deleteCar(@PathVariable Long carId) {
        carService.deleteCar(carId);
        return ResponseEntity.noContent().build();
    }

    // ---- Discovery ----
    @Operation(summary = "Search cars", description = "Search cars using filter criteria (brand, price range, fuel type, etc.)")
    @GetMapping("/searchCars")
    public ResponseEntity<List<CarDTO>> searchCars(CarSearchFilter filter) {
        return ResponseEntity.ok(carService.searchCars(filter));
    }

    @Operation(summary = "Get ML-suggested price for a car")
    @GetMapping("/suggestPrice")
    public ResponseEntity<Double> suggestPrice(@RequestParam Long carId) {
        return ResponseEntity.ok(carService.suggestPrice(carId));
    }


    @Operation(summary = "Get car recommendations for a customer")
    @GetMapping("/recommendations/{customerId}")
    public ResponseEntity<List<CarDTO>> recommendations(@PathVariable String customerId) {
        return ResponseEntity.ok(carService.getRecommendations(customerId));
    }

    @Operation(summary = "Download an uploaded car document/image", description = "Downloads a file by filename from the uploads directory")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File found",
                    content = @Content(mediaType = "application/octet-stream")),
            @ApiResponse(responseCode = "404", description = "File not found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Malformed file path", content = @Content)
    })
    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get("uploads").resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Upload a car picture")
    @PostMapping(value = "/uploadCarPictures/{carId}", consumes = "multipart/form-data")
    public ResponseEntity<Void> uploadCarPictures(
            @PathVariable Long carId,
            @RequestParam(required = false) MultipartFile img) {
        carService.uploadCarPictures(carId, img);
        return ResponseEntity.ok().build();
    }
        //get puc, insurance, rc
        //

    @Operation(summary = "Delete a car picture")
    @DeleteMapping("/deleteCarPictures/{carId}")
    public ResponseEntity<Void> deleteCarPictures(
            @PathVariable Long carId,
            @RequestParam Long imgId){
        carService.deleteCarPicture(carId, imgId);
        return ResponseEntity.ok().build();
    }


    //Payments
    @Operation(summary = "Get car details for payment processing")
    @GetMapping("/viewCarPayments/{carId}")
    public CarResponse getCarForPayments(@PathVariable Long carId) {
        CarResponse carResponse = carService.getCarForPayments(carId);
        return carResponse;
    }

    @Operation(summary = "Get car details for scheduling")
    @GetMapping("/viewCarSched/{carId}")
    public CarDTOSched getCarForSched(@PathVariable Long carId) {
        System.out.println("hello in scheduling");
        CarDTOSched carResponse = carService.getCarForScheduling(carId);
        return carResponse;
    }
}
