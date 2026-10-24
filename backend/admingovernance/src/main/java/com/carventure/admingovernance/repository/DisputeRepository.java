package com.carventure.admingovernance.repository;

import com.carventure.admingovernance.model.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {
}