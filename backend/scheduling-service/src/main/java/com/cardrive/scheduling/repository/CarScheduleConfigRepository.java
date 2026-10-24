package com.cardrive.scheduling.repository;

import com.cardrive.scheduling.domain.entity.CarScheduleConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarScheduleConfigRepository extends JpaRepository<CarScheduleConfig, Long> {
}
