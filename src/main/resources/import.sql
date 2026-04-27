CREATE UNIQUE INDEX IF NOT EXISTS idx_payment_reservation_active
    ON p_payment (reservation_id)
    WHERE status IN ('INIT', 'IN_PROGRESS');