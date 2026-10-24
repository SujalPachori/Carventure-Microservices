package com.autoplatform.payment.webhook;

import com.autoplatform.payment.config.StripeProperties;
import com.autoplatform.payment.exception.InvalidWebhookSignatureException;
import com.autoplatform.payment.repository.ProcessedWebhookEventRepository;
import com.autoplatform.payment.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Verifies the webhook endpoint rejects payloads with a bad/missing Stripe
 * signature before ever touching the order/payment repositories - the
 * single most important thing to get right about a payment webhook, since
 * an unverified endpoint lets anyone forge "payment succeeded" events.
 */
@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

    @Mock private ProcessedWebhookEventRepository processedWebhookEventRepository;
    @Mock private OrderService orderService;

    @Test
    @DisplayName("rejects a payload with an invalid signature and never touches order state")
    void rejectsInvalidSignature() {
        StripeProperties properties = new StripeProperties("sk_test_dummy", "pk_test_dummy", "whsec_realsecret");
        StripeWebhookService service = new StripeWebhookService(properties, processedWebhookEventRepository, orderService);

        String payload = "{\"id\":\"evt_test\",\"type\":\"payment_intent.succeeded\"}";
        String forgedSignature = "t=1000000000,v1=not_a_real_signature";

        assertThatThrownBy(() -> service.handle(payload, forgedSignature))
                .isInstanceOf(InvalidWebhookSignatureException.class);

        verifyNoInteractions(orderService, processedWebhookEventRepository);
    }
}
