package com.carventure.engagement.repository;

import com.carventure.engagement.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<WishlistItem, String> {

    List<WishlistItem> findByCustomerIdOrderByAddedAtDesc(String customerId);

    Optional<WishlistItem> findByCustomerIdAndCarId(String customerId, String carId);

    boolean existsByCustomerIdAndCarId(String customerId, String carId);

    @Modifying
    @Query("delete from WishlistItem w where w.customerId = :customerId and w.carId = :carId")
    int deleteByCustomerIdAndCarId(@Param("customerId") String customerId, @Param("carId") String carId);
}
