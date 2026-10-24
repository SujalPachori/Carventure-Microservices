ALTER TABLE orders ADD COLUMN expires_at TIMESTAMPTZ;

CREATE INDEX idx_orders_expiry_scan ON orders (status, expires_at)
    WHERE status IN ('CREATED', 'AWAITING_PAYMENT');