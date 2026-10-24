-- ============================================================
-- orders: one row per purchase attempt
-- ============================================================
CREATE TABLE orders (
    id                  BIGSERIAL PRIMARY KEY,
    buyer_id            BIGINT       NOT NULL,
    seller_id           BIGINT       NOT NULL,
    car_id              BIGINT       NOT NULL,
    total_amount        NUMERIC(12,2) NOT NULL,
    down_payment_amount NUMERIC(12,2),
    currency            VARCHAR(3)   NOT NULL,
    payment_type        VARCHAR(20)  NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version             BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_orders_buyer ON orders (buyer_id);
CREATE INDEX idx_orders_seller ON orders (seller_id);
CREATE INDEX idx_orders_car ON orders (car_id);

-- The authoritative guarantee: a car can have at most one order that is ever
-- PAID or FINANCING_ACTIVE. This holds even under concurrent webhook
-- deliveries / concurrent transactions - Postgres rejects the second
-- conflicting UPDATE outright, and OrderServiceImpl.completePayment()
-- catches that and automatically refunds the losing payment.
CREATE UNIQUE INDEX uk_orders_car_sold ON orders (car_id) WHERE status IN ('PAID', 'FINANCING_ACTIVE');

-- ============================================================
-- payments: one row per Stripe PaymentIntent
-- ============================================================
CREATE TABLE payments (
    id                        BIGSERIAL PRIMARY KEY,
    order_id                  BIGINT       NOT NULL REFERENCES orders (id),
    stripe_payment_intent_id  VARCHAR(100) NOT NULL UNIQUE,
    amount                    NUMERIC(12,2) NOT NULL,
    currency                  VARCHAR(3)   NOT NULL,
    status                    VARCHAR(30)  NOT NULL,
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version                   BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_payments_order ON payments (order_id);

-- ============================================================
-- financing_plans: one per financed order, created once the down payment succeeds
-- ============================================================
CREATE TABLE financing_plans (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT       NOT NULL UNIQUE REFERENCES orders (id),
    principal_amount    NUMERIC(12,2) NOT NULL,
    tenure_months       INT          NOT NULL,
    monthly_installment NUMERIC(12,2) NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ============================================================
-- processed_webhook_events: idempotency guard for Stripe webhook deliveries
-- ============================================================
CREATE TABLE processed_webhook_events (
    stripe_event_id VARCHAR(100) PRIMARY KEY,
    event_type      VARCHAR(100) NOT NULL,
    processed_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
