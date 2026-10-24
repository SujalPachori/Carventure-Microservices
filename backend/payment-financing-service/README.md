# Payment & Financing Service

Handles a buyer purchasing a car — full payment or financing — via Stripe, and tells
the seller their car sold.

```
Buyer → Payment & Financing Service → Stripe (charge)
                    │
                    ├── Order / Payment / FinancingPlan lifecycle
                    └── Kafka: CAR_SOLD → Notification Service ("your car is sold")
                                        → Car Service (delist the car)
```

## Why this design

**A car can be sold at most once — enforced the same two-layer way as the Scheduling
Service's double-booking guarantee**, because it's the same shape of problem
(concurrent requests racing to claim one scarce resource):

1. **Application layer** — `createOrder` checks for an existing active/completed order
   for the car before creating a new one.
2. **Database layer** — a partial unique index, `orders(car_id) WHERE status IN
   ('PAID','FINANCING_ACTIVE')` (see `V1__init.sql`). Postgres physically rejects a
   second row reaching either of those statuses for the same car.

The interesting case unique to *payments* (vs. scheduling): by the time the DB
constraint fires, **money may already have been captured by Stripe**. So
`OrderServiceImpl.completePayment()` catches the constraint violation and
automatically **refunds** the losing payment and marks that order `REFUNDED`, rather
than leaving a buyer charged for a car they didn't get. `OrderSaleConflictIntegrationTest`
proves this end-to-end against a real Postgres.

**Car validation** reuses the same fix just applied to the Scheduling Service: a Feign
client (`client/CarServiceClient`) + Resilience4j circuit breaker/retry
(`client/CarValidationService`) fetches the real car (price, seller, listed status)
from Car Service before trusting anything the buyer's request claims — a car's
`sellerId` and price are never taken from client input.

**Financing** is intentionally simple in V1: buyer pays a down payment (default 20%,
configurable) via Stripe now, and a `FinancingPlan` row records an interest-free equal
monthly installment schedule (default 36 months). There's no loan servicing,
collections, or interest calculation here — swap in a real financing partner
integration behind `OrderService`/`FinancingPlan` later without touching the payment
or car-sold-notification flow.

## Stack

Java 21 · Spring Boot 3.3 · Spring Data JPA · Spring Kafka · Spring Cloud OpenFeign +
Resilience4j · Stripe Java SDK 33.3.0 · PostgreSQL 16 · Flyway · springdoc-openapi ·
Spring AOP · JUnit 5 / Mockito / Testcontainers

## Project layout

```
config/     StripeConfig/Properties, FinancingProperties, Kafka topics, OpenAPI, JPA auditing
aop/        LoggingAspect (service-layer timing), AuditAspect (financial audit trail on Order changes)
controller/ OrderController, StripeWebhookController
client/     CarServiceClient (Feign) + CarValidationService (circuit breaker + retry)
service/    OrderService (purchase lifecycle), PaymentService (Stripe wrapper)
webhook/    StripeWebhookService — signature verification + idempotent event dispatch
entity/     Order, Payment, FinancingPlan, ProcessedWebhookEvent
event/      OrderEvent (order-events topic) + CarSoldEvent (car-sold-events topic) + producer
exception/  domain exceptions + GlobalExceptionHandler (RFC 7807 ProblemDetail)
```

## Running locally

```bash
export STRIPE_SECRET_KEY=sk_test_...
export STRIPE_PUBLISHABLE_KEY=pk_test_...
export STRIPE_WEBHOOK_SECRET=whsec_...      # from `stripe listen` (see below)
docker compose up --build
```

- Swagger UI: http://localhost:8082/swagger-ui.html
- Set `CAR_SERVICE_URL` to point at your real Car Service (defaults to
  `http://car-service:8081` inside compose).

### Forwarding Stripe webhooks locally

```bash
stripe listen --forward-to localhost:8082/api/v1/payments/webhook
```

Copy the `whsec_...` it prints into `STRIPE_WEBHOOK_SECRET`.

### Example flow

```bash
# Buyer starts a full-payment purchase
curl -X POST localhost:8082/api/v1/orders \
  -H 'Content-Type: application/json' \
  -d '{"buyerId":1,"carId":100,"paymentType":"FULL_PAYMENT"}'
# → { "order": {...}, "stripeClientSecret": "pi_..._secret_...", ... }

# Buyer's frontend confirms payment with stripeClientSecret via Stripe.js/Elements.
# Stripe then calls this service's webhook, which marks the order PAID and
# publishes CAR_SOLD - no further API call needed here.

# Financed purchase: 25% down over 48 months
curl -X POST localhost:8082/api/v1/orders \
  -H 'Content-Type: application/json' \
  -d '{"buyerId":1,"carId":101,"paymentType":"FINANCING","downPaymentPercent":25,"tenureMonths":48}'
```

## Kafka events

- **`order-events`** (key = `carId`): `ORDER_CREATED`, `ORDER_PAID`,
  `ORDER_FINANCING_ACTIVATED`, `ORDER_PAYMENT_FAILED`, `ORDER_CANCELLED`, `ORDER_REFUNDED`.
- **`car-sold-events`** (key = `carId`): a single `CarSoldEvent` published exactly when
  a car's sale is finalized. **This is the event that answers "notify the seller their
  car is sold"** — the Notification Service consumes it to message the seller, and Car
  Service consumes it to delist the car. Kept as its own topic (rather than folded into
  `order-events`) because it has different consumers and matters more.

Same caveat as the Scheduling Service: publishing is fire-and-forget inside the
`@Transactional` method, not a transactional outbox — acceptable for V1, noted as the
next step for exactly-once delivery guarantees.

## Idempotency, twice over

- **Stripe retries**: `PaymentIntent`/`Refund` creation both pass a Stripe
  `idempotencyKey` derived from the order id, so a retried "create order" call or a
  network-timeout retry never double-charges or double-refunds.
- **Webhook redelivery**: `processed_webhook_events` stores the Stripe event id as its
  primary key — a redelivered event fails that insert and is skipped before touching
  any order. `completePayment`/`failPayment` are also independently idempotent (no-op
  if the order is already in a terminal/sold state) as a second line of defense.

## Testing

```bash
mvn test
```

- `service/OrderServiceImplTest` — Mockito: car-not-listed, car-already-sold, down-payment
  math, the "refund the loser" path, and idempotent no-op on a redelivered webhook.
- `webhook/StripeWebhookServiceTest` — confirms a forged/invalid signature is rejected
  before any order state is touched.
- `integration/OrderSaleConflictIntegrationTest` — **Testcontainers + real Postgres.**
  Fires two orders for the same car through `completePayment` concurrently and asserts
  exactly one ends `PAID` and the other `REFUNDED` — this is what actually proves the
  partial unique index + refund logic work together, not just each in isolation.
- `controller/OrderControllerTest` — `@WebMvcTest` slice test for HTTP status/JSON shape.

Requires Docker (Testcontainers) for the integration test. Stripe/Car Service calls are
mocked throughout — no test hits the real Stripe API or a real Car Service.

## Known simplifications (V1)

- Financing has no interest, no missed-payment handling, and no loan servicing beyond
  the initial `FinancingPlan` row — see "Why this design" above.
- No authN/authZ — `buyerId` is passed as a plain request param and trusted as-is; wire
  up real auth before production use.
- `getClientSecret` re-fetches the PaymentIntent from Stripe on every call rather than
  caching it; fine at this volume, but cache or store it at creation time if this
  becomes a hot path.
