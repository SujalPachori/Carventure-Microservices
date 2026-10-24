package com.autoplatform.payment.controller;

import com.autoplatform.payment.webhook.StripeWebhookService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stripe webhook endpoint. Hidden from OpenAPI (Stripe, not a client, calls
 * this) and deliberately reads the RAW request body as a String - Stripe's
 * signature is computed over the exact bytes sent, so any JSON
 * parsing/re-serialization before verification would break it.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Hidden
public class StripeWebhookController {

    private final StripeWebhookService stripeWebhookService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody String payload,
                                               @RequestHeader("Stripe-Signature") String signature) {
        stripeWebhookService.handle(payload, signature);
        return ResponseEntity.ok().build();
    }
}
