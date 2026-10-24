package com.autoplatform.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "offer-negotiation-service")
public interface OfferServiceClient {

    @GetMapping("/api/v1/offers/{offerId}")
    OfferResponse getOffer(@PathVariable("offerId") String offerId);
}