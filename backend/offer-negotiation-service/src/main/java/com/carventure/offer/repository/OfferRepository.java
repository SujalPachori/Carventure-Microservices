package com.carventure.offer.repository;

import com.carventure.offer.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OfferRepository extends JpaRepository<Offer, String> {

    List<Offer> findByCarIdOrderByCreatedAtDesc(String carId);

    List<Offer> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<Offer> findBySellerIdOrderByCreatedAtDesc(String sellerId);

    boolean existsByCarIdAndCustomerIdAndStatus(String carId, String customerId, String status);

    @Modifying
    @Query("update Offer o set o.status = 'CLOSED', o.respondedAt = :now "
            + "where o.carId = :carId and o.status = 'PENDING' and o.offerId <> :keepOfferId")
    int closeRivalOffers(@Param("carId") String carId,
                         @Param("keepOfferId") String keepOfferId,
                         @Param("now") LocalDateTime now);

    @Query("select o from Offer o where o.carId = :carId and o.status = 'PENDING' and o.offerId <> :keepOfferId")
    List<Offer> findRivalOffers(@Param("carId") String carId, @Param("keepOfferId") String keepOfferId);

    @Query("select o from Offer o where o.status = 'ACCEPTED' and o.respondedAt < :cutoff")
    List<Offer> findStaleAcceptedOffers(@Param("cutoff") LocalDateTime cutoff);
}
