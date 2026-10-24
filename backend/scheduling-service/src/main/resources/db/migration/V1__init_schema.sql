-- ============================================================================
-- V1: Base schema for the Scheduling Service
--
-- Identity is username-keyed platform-wide, matching auth-service: its User
-- entity's primary key IS the username (no numeric user id exists anywhere
-- in the platform), so seller/buyer references here are VARCHAR usernames,
-- not BIGINT ids. car_id stays BIGINT - that PK is owned by the separate
-- Car Discovery Service and is unrelated to user identity.
-- ============================================================================

CREATE TABLE availability (
    id                BIGSERIAL PRIMARY KEY,
    car_id            BIGINT      NOT NULL,
    seller_username   VARCHAR(100) NOT NULL,
    day_of_week       VARCHAR(16) NOT NULL,
    start_time        TIME        NOT NULL,
    end_time          TIME        NOT NULL,
    active            BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    version           BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT chk_availability_window CHECK (start_time < end_time)
);

CREATE INDEX idx_availability_car_day ON availability (car_id, day_of_week) WHERE active = TRUE;
CREATE INDEX idx_availability_seller ON availability (seller_username);

CREATE TABLE car_schedule_config (
    car_id            BIGINT PRIMARY KEY,
    seller_username   VARCHAR(100) NOT NULL,
    duration_minutes  INTEGER NOT NULL DEFAULT 45 CHECK (duration_minutes > 0)
);

CREATE TABLE test_drive (
    id                    BIGSERIAL PRIMARY KEY,
    car_id                BIGINT      NOT NULL,
    seller_username       VARCHAR(100) NOT NULL,
    buyer_username        VARCHAR(100) NOT NULL,
    start_time            TIMESTAMP   NOT NULL,
    end_time              TIMESTAMP   NOT NULL,
    status                VARCHAR(16) NOT NULL,
    rejection_reason      TEXT,
    cancellation_reason   TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    version               BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT chk_test_drive_window CHECK (start_time < end_time),
    CONSTRAINT chk_test_drive_status CHECK (status IN ('PENDING','CONFIRMED','REJECTED','CANCELLED','COMPLETED'))
);

CREATE INDEX idx_test_drive_car ON test_drive (car_id);
CREATE INDEX idx_test_drive_buyer ON test_drive (buyer_username);
CREATE INDEX idx_test_drive_seller ON test_drive (seller_username);
CREATE INDEX idx_test_drive_status ON test_drive (status);

CREATE TABLE outbox_event (
    id             BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id   VARCHAR(64) NOT NULL,
    event_type     VARCHAR(64) NOT NULL,
    payload        JSONB       NOT NULL,
    published      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox_event (id) WHERE published = FALSE;
