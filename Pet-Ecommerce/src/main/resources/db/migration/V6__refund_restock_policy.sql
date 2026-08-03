ALTER TABLE refund_requests
    ADD COLUMN restock_required BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE refund_requests
SET restock_required = FALSE
WHERE reason = 'VNPay payment was confirmed after the order was cancelled'
  AND status <> 'COMPLETED';