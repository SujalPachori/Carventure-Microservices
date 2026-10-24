package com.carventure.offer.service;

import com.carventure.offer.client.CarLookup;
import com.carventure.offer.client.UserLookup;
import com.carventure.offer.dto.CarResponse;
import com.carventure.offer.dto.CreateOfferRequest;
import com.carventure.offer.dto.OfferResponse;
import com.carventure.offer.dto.RespondOfferRequest;
import com.carventure.offer.dto.UserResponse;
import com.carventure.offer.entity.CarDeal;
import com.carventure.offer.entity.Offer;
import com.carventure.offer.exception.CarAlreadySoldException;
import com.carventure.offer.exception.InvalidOfferStateException;
import com.carventure.offer.exception.NotAllowedException;
import com.carventure.offer.exception.OfferNotFoundException;
import com.carventure.offer.repository.CarDealRepository;
import com.carventure.offer.repository.OfferRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final CarDealRepository carDealRepository;
    private final CarLookup carLookup;
    private final UserLookup userLookup;
    private final OfferEventPublisher publisher;

    public OfferService(OfferRepository offerRepository,
                        CarDealRepository carDealRepository,
                        CarLookup carLookup,
                        UserLookup userLookup,
                        OfferEventPublisher publisher) {
        this.offerRepository = offerRepository;
        this.carDealRepository = carDealRepository;
        this.carLookup = carLookup;
        this.userLookup = userLookup;
        this.publisher = publisher;
    }

    @Transactional
    public OfferResponse createOffer(CreateOfferRequest request) {

        if (carDealRepository.existsById(request.getCarId())) {
            throw new CarAlreadySoldException("This car already has an accepted offer");
        }

        if (offerRepository.existsByCarIdAndCustomerIdAndStatus(
                request.getCarId(), request.getCustomerId(), "PENDING")) {
            throw new InvalidOfferStateException("You already have a pending offer on this car");
        }

        CarResponse car = carLookup.getCar(request.getCarId());

        if (car == null) {
            throw new OfferNotFoundException("Car not found: " + request.getCarId());
        }

        Offer offer = new Offer();
        offer.setOfferId(UUID.randomUUID().toString());
        offer.setCarId(request.getCarId());
        offer.setSellerId(car.resolveSellerIdentity());
        offer.setCustomerId(request.getCustomerId());
        offer.setOfferedAmount(request.getOfferedAmount());
        offer.setOfferedBy("CUSTOMER");
        offer.setStatus("PENDING");
        offer.setCreatedAt(LocalDateTime.now());

        Offer saved = offerRepository.save(offer);

        publisher.publish("OFFER_RECEIVED", saved, saved.getSellerId());

        return enrich(OfferResponse.from(saved), car);
    }

    @Transactional
    public OfferResponse accept(String offerId, String actorId) {

        Offer offer = load(offerId);
        requirePending(offer);

        if (!offer.getSellerId().equals(actorId) && !offer.getCustomerId().equals(actorId)) {
            throw new NotAllowedException("You are not a party to this offer");
        }

        if (offer.getOfferedBy().equals("CUSTOMER") && !offer.getSellerId().equals(actorId)) {
            throw new NotAllowedException("Only the seller can accept a customer offer");
        }

        if (offer.getOfferedBy().equals("SELLER") && !offer.getCustomerId().equals(actorId)) {
            throw new NotAllowedException("Only the customer can accept a seller offer");
        }

        LocalDateTime now = LocalDateTime.now();

        CarDeal deal = new CarDeal();
        deal.setCarId(offer.getCarId());
        deal.setAcceptedOfferId(offer.getOfferId());
        deal.setAcceptedBuyerId(offer.getCustomerId());
        deal.setSellerId(offer.getSellerId());
        deal.setAgreedAmount(offer.getOfferedAmount());
        deal.setAcceptedAt(now);

        try {
            carDealRepository.saveAndFlush(deal);
        } catch (DataIntegrityViolationException ex) {
            throw new CarAlreadySoldException("This car already has an accepted offer");
        }

        List<Offer> rivals = offerRepository.findRivalOffers(offer.getCarId(), offer.getOfferId());

        offerRepository.closeRivalOffers(offer.getCarId(), offer.getOfferId(), now);

        offer.setStatus("ACCEPTED");
        offer.setRespondedAt(now);
        Offer saved = offerRepository.save(offer);

        carLookup.markReserved(offer.getCarId());

        publisher.publish("OFFER_ACCEPTED", saved, otherParty(saved, actorId));

        for (Offer rival : rivals) {
            rival.setStatus("CLOSED");
            publisher.publish("OFFER_CLOSED", rival, rival.getCustomerId());
        }

        return enrich(OfferResponse.from(saved), carLookup.getCar(saved.getCarId()));
    }

    @Transactional
    public OfferResponse reject(String offerId, String actorId) {

        Offer offer = load(offerId);
        requirePending(offer);
        requireResponder(offer, actorId);

        offer.setStatus("REJECTED");
        offer.setRespondedAt(LocalDateTime.now());

        Offer saved = offerRepository.save(offer);

        publisher.publish("OFFER_REJECTED", saved, otherParty(saved, actorId));

        return enrich(OfferResponse.from(saved), carLookup.getCar(saved.getCarId()));
    }

    @Transactional
    public OfferResponse counter(String offerId, RespondOfferRequest request) {

        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new InvalidOfferStateException("Counter amount must be positive");
        }

        Offer existing = load(offerId);
        requirePending(existing);
        requireResponder(existing, request.getActorId());

        if (carDealRepository.existsById(existing.getCarId())) {
            throw new CarAlreadySoldException("This car already has an accepted offer");
        }

        LocalDateTime now = LocalDateTime.now();

        existing.setStatus("COUNTERED");
        existing.setRespondedAt(now);
        offerRepository.save(existing);

        String counteredBy = existing.getOfferedBy().equals("CUSTOMER") ? "SELLER" : "CUSTOMER";

        Offer counter = new Offer();
        counter.setOfferId(UUID.randomUUID().toString());
        counter.setParentOfferId(existing.getOfferId());
        counter.setCarId(existing.getCarId());
        counter.setSellerId(existing.getSellerId());
        counter.setCustomerId(existing.getCustomerId());
        counter.setOfferedAmount(request.getAmount());
        counter.setOfferedBy(counteredBy);
        counter.setStatus("PENDING");
        counter.setCreatedAt(now);

        Offer saved = offerRepository.save(counter);

        String recipient = counteredBy.equals("SELLER") ? saved.getCustomerId() : saved.getSellerId();
        publisher.publish("OFFER_COUNTERED", saved, recipient);

        return enrich(OfferResponse.from(saved), carLookup.getCar(saved.getCarId()));
    }

    @Transactional
    public void withdraw(String offerId, String actorId) {

        Offer offer = load(offerId);
        requirePending(offer);

        if (!offer.getCustomerId().equals(actorId)) {
            throw new NotAllowedException("Only the buyer can withdraw this offer");
        }

        offer.setStatus("CLOSED");
        offer.setRespondedAt(LocalDateTime.now());
        offerRepository.save(offer);
    }

    public List<OfferResponse> byCar(String carId) {
        return enrichAll(offerRepository.findByCarIdOrderByCreatedAtDesc(carId));
    }

    public List<OfferResponse> byCustomer(String customerId) {
        return enrichAll(offerRepository.findByCustomerIdOrderByCreatedAtDesc(customerId));
    }

    public List<OfferResponse> bySeller(String sellerId) {
        return enrichAll(offerRepository.findBySellerIdOrderByCreatedAtDesc(sellerId));
    }

    public OfferResponse byId(String offerId) {
        Offer offer = load(offerId);
        return enrich(OfferResponse.from(offer), carLookup.getCar(offer.getCarId()));
    }

    private List<OfferResponse> enrichAll(List<Offer> offers) {
        return offers.stream()
                .map(offer -> enrich(OfferResponse.from(offer), carLookup.getCar(offer.getCarId())))
                .toList();
    }

    private OfferResponse enrich(OfferResponse response, CarResponse car) {

        if (car != null) {
            response.setCarBrand(car.getBrand());
            response.setCarModel(car.getModel());
            response.setCarPrice(car.getPrice());
            if (car.getImageUrls() != null && !car.getImageUrls().isEmpty()) {
                response.setCarImageUrl(car.getImageUrls().get(0));
            }
        }

        UserResponse user = userLookup.getCustomer(response.getCustomerId());

        if (user != null) {
            response.setCustomerName(user.getFirstName() + " " + user.getLastName());
        }

        return response;
    }

    private Offer load(String offerId) {
        return offerRepository.findById(offerId)
                .orElseThrow(() -> new OfferNotFoundException("Offer not found: " + offerId));
    }

    private void requirePending(Offer offer) {
        if (!"PENDING".equals(offer.getStatus())) {
            throw new InvalidOfferStateException("Offer is already " + offer.getStatus());
        }
    }

    private void requireResponder(Offer offer, String actorId) {
        String responder = offer.getOfferedBy().equals("CUSTOMER")
                ? offer.getSellerId()
                : offer.getCustomerId();

        if (!responder.equals(actorId)) {
            throw new NotAllowedException("You cannot respond to this offer");
        }
    }

    private String otherParty(Offer offer, String actorId) {
        return offer.getSellerId().equals(actorId) ? offer.getCustomerId() : offer.getSellerId();
    }
}
