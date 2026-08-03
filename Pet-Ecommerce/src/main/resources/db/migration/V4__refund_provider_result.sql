-- Persist non-sensitive refund provider results for support and reconciliation.
ALTER TABLE refund_requests
    ADD COLUMN provider_response_code VARCHAR(30) NULL,
    ADD COLUMN provider_raw_response TEXT NULL,
    ADD COLUMN processed_by BIGINT NULL,
    ADD CONSTRAINT fk_refund_processed_by
        FOREIGN KEY (processed_by) REFERENCES users(id);

-- Backfill transaction time required when refunding older VNPay payments.
UPDATE payments
SET provider_paid_at = paid_at
WHERE provider = 'VNPAY'
  AND status = 'PAID'
  AND provider_paid_at IS NULL;
