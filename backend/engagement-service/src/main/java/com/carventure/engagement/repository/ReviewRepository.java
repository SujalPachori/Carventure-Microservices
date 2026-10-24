package com.carventure.engagement.repository;

import com.carventure.engagement.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, String> {

    Page<Review> findBySellerIdOrderByCreatedAtDesc(String sellerId, Pageable pageable);

    Optional<Review> findBySellerIdAndCustomerId(String sellerId, String customerId);

    long countBySellerId(String sellerId);

    @Query("select avg(r.rating) from Review r where r.sellerId = :sellerId")
    Double averageRating(@Param("sellerId") String sellerId);
}
