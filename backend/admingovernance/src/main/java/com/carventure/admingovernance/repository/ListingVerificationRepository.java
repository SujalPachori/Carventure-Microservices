package com.carventure.admingovernance.repository;

import com.carventure.admingovernance.model.ListingVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListingVerificationRepository extends JpaRepository<ListingVerification, Long> {

    List<ListingVerification> findByCarIdOrderByVerifiedDateDesc(Long carId);
}