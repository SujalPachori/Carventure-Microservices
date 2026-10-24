package com.carventure.offer.controller;

import com.carventure.offer.dto.CreateOfferRequest;
import com.carventure.offer.dto.OfferResponse;
import com.carventure.offer.dto.RespondOfferRequest;
import com.carventure.offer.service.OfferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/offers")
public class OfferController {

    private final OfferService service;

    public OfferController(OfferService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OfferResponse> create(@Valid @RequestBody CreateOfferRequest request) {
        return new ResponseEntity<>(service.createOffer(request), HttpStatus.CREATED);
    }

    @GetMapping("/{offerId}")
    public OfferResponse byId(@PathVariable String offerId) {
        return service.byId(offerId);
    }

    @GetMapping("/car/{carId}")
    public List<OfferResponse> byCar(@PathVariable String carId) {
        return service.byCar(carId);
    }

    @GetMapping("/customer/{customerId}")
    public List<OfferResponse> byCustomer(@PathVariable String customerId) {
        return service.byCustomer(customerId);
    }

    @GetMapping("/seller/{sellerId}")
    public List<OfferResponse> bySeller(@PathVariable String sellerId) {
        return service.bySeller(sellerId);
    }

    @PutMapping("/{offerId}/accept")
    public OfferResponse accept(@PathVariable String offerId, @RequestParam String actorId) {
        return service.accept(offerId, actorId);
    }

    @PutMapping("/{offerId}/reject")
    public OfferResponse reject(@PathVariable String offerId, @RequestParam String actorId) {
        return service.reject(offerId, actorId);
    }

    @PutMapping("/{offerId}/counter")
    public OfferResponse counter(@PathVariable String offerId,
                                 @Valid @RequestBody RespondOfferRequest request) {
        return service.counter(offerId, request);
    }

    @DeleteMapping("/{offerId}")
    public ResponseEntity<Void> withdraw(@PathVariable String offerId, @RequestParam String actorId) {
        service.withdraw(offerId, actorId);
        return ResponseEntity.noContent().build();
    }
}
