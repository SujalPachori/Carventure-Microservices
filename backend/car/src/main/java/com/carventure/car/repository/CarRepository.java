package com.carventure.car.repository;

import com.carventure.car.model.Car;
import com.carventure.car.model.FuelType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Year;
import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> {

    // used by viewAllCarsBySellerId
    List<Car> findBySellerUsername(String sellerUsername);

    // used by searchCars — every param is optional (null = "don't filter on this")
    @Query("""
        SELECT c FROM Car c
        WHERE (:brand IS NULL OR LOWER(c.brand) LIKE LOWER(CONCAT('%', CAST(:brand AS string), '%')))
          AND (:model IS NULL OR LOWER(c.model) LIKE LOWER(CONCAT('%', CAST(:model AS string), '%')))
          AND (:minPrice IS NULL OR c.price >= :minPrice)
          AND (:maxPrice IS NULL OR c.price <= :maxPrice)
          AND (:year IS NULL OR c.manufactureYear = :year)
          AND (:maxMileage IS NULL OR c.mileage <= :maxMileage)
          AND (:fuelType IS NULL OR c.fuelType = :fuelType)
          AND (:transmission IS NULL OR LOWER(c.transmission) LIKE LOWER(CONCAT('%', CAST(:transmission AS string), '%')))
          AND (:location IS NULL OR LOWER(c.location) LIKE LOWER(CONCAT('%', CAST(:location AS string), '%')))
        """)
    List<Car> searchCars(
            @Param("brand") String brand,
            @Param("model") String model,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("year") Year year,
            @Param("maxMileage") Integer maxMileage,
            @Param("fuelType") FuelType fuelType,
            @Param("transmission") String transmission,
            @Param("location") String location
    );

    // used by suggestPrice — comparable listings: same brand/model/fuel type, active status
    @Query("""
            SELECT c FROM Car c
            WHERE c.brand = :brand
              AND c.model = :model
              AND c.status = 'Active'
            """)
    List<Car> findComparableListings(
            @Param("brand") String brand,
            @Param("model") String model
    );

    // used by recommendations — placeholder heuristic until a real
    // recommendation engine/service exists; returns the latest Active listings
    List<Car> findTop10ByStatusOrderByPostedDateDesc(String status);
}