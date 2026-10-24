package com.cardrive.scheduling.repository;

import com.cardrive.scheduling.domain.entity.Availability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    List<Availability> findByCarIdOrderByDayOfWeekAscStartTimeAsc(Long carId);

    List<Availability> findByCarIdAndDayOfWeekAndActiveTrue(Long carId, DayOfWeek dayOfWeek);

    List<Availability> findBySellerUsername(String sellerUsername);
}
