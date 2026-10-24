package com.carventure.offer.service;

import com.carventure.offer.client.CarLookup;
import com.carventure.offer.entity.Offer;
import com.carventure.offer.repository.CarDealRepository;
import com.carventure.offer.repository.OfferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReservationExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryJob.class);

    private final OfferRepository offerRepository;
    private final CarDealRepository carDealRepository;
    private final CarLookup carLookup;
    private final OfferEventPublisher publisher;

    @Value("${offer.reservation.expiry-minutes}")
    private long expiryMinutes;

    public ReservationExpiryJob(OfferRepository offerRepository,
                                CarDealRepository carDealRepository,
                                CarLookup carLookup,
                                OfferEventPublisher publisher) {
        this.offerRepository = offerRepository;
        this.carDealRepository = carDealRepository;
        this.carLookup = carLookup;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${offer.reservation.sweep-interval-ms}")
    @Transactional
    public void releaseStaleReservations() {

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(expiryMinutes);
        List<Offer> stale = offerRepository.findStaleAcceptedOffers(cutoff);

        for (Offer offer : stale) {

            var car = carLookup.getCar(offer.getCarId());
            if (car == null) {
                log.warn("Skipping offer {} - car service unavailable", offer.getOfferId());
                continue;
            }
            if (!"RESERVED".equalsIgnoreCase(car.getStatus())) {
                log.debug("Offer {} car {} is {} - not releasing",
                        offer.getOfferId(), offer.getCarId(), car.getStatus());
                continue;
            }

            offer.setStatus("CLOSED");
            offer.setRespondedAt(LocalDateTime.now());
            offerRepository.save(offer);

            carDealRepository.deleteById(offer.getCarId());

            carLookup.markListed(offer.getCarId());

            publisher.publish("OFFER_EXPIRED", offer, offer.getCustomerId());
            publisher.publish("OFFER_EXPIRED", offer, offer.getSellerId());

            log.info("Released reservation on car {} - offer {} expired unpaid",
                    offer.getCarId(), offer.getOfferId());
        }
    }
}