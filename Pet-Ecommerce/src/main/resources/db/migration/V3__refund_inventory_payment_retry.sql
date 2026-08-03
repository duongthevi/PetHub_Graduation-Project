-- =========================================================
-- 1. Chuẩn hóa dữ liệu role
-- =========================================================

UPDATE users
SET role = 'USER'
WHERE role IS NULL
   OR TRIM(CAST(role AS CHAR)) = ''
   OR role NOT IN ('ADMIN', 'USER');

ALTER TABLE users
    MODIFY COLUMN role ENUM('ADMIN', 'USER') NOT NULL;


-- =========================================================
-- 2. Hoàn thiện refund_requests
-- V2 đang dùng PENDING nhưng Java enum dùng REQUESTED
-- =========================================================

UPDATE refund_requests
SET status = 'REQUESTED'
WHERE status = 'PENDING';

ALTER TABLE refund_requests
    MODIFY COLUMN status VARCHAR(20)
        NOT NULL DEFAULT 'REQUESTED',
    ADD COLUMN provider_request_id VARCHAR(100) NULL
        AFTER status,
    ADD COLUMN reviewed_by BIGINT NULL
        AFTER requested_by,
    ADD COLUMN reviewed_at DATETIME(6) NULL
        AFTER updated_at,
    ADD COLUMN processing_started_at DATETIME(6) NULL
        AFTER reviewed_at,
    ADD COLUMN last_error TEXT NULL
        AFTER admin_note;

CREATE UNIQUE INDEX uk_refund_provider_request
    ON refund_requests(provider_request_id);

CREATE UNIQUE INDEX uk_refund_provider_ref
    ON refund_requests(provider_refund_ref);

CREATE INDEX idx_refund_status_created
    ON refund_requests(status, created_at);

ALTER TABLE refund_requests
    ADD CONSTRAINT fk_refund_reviewed_by
        FOREIGN KEY (reviewed_by) REFERENCES users(id);


-- =========================================================
-- 3. Lưu chính xác thời điểm giao hàng và thời điểm VNPay trả tiền
-- =========================================================

ALTER TABLE orders
    ADD COLUMN delivered_at DATETIME(6) NULL
        AFTER cancelled_at;

UPDATE orders
SET delivered_at = COALESCE(updated_at, created_at)
WHERE order_status IN ('DELIVERED', 'RETURNED')
  AND delivered_at IS NULL;

ALTER TABLE payments
    ADD COLUMN provider_paid_at DATETIME(6) NULL
        AFTER paid_at;


-- =========================================================
-- 4. Hoàn thiện inventory transaction
-- balance_after dùng để biết tồn kho sau giao dịch
-- =========================================================

ALTER TABLE inventory_transactions
    ADD COLUMN balance_after INT NULL
        AFTER quantity_changed;

CREATE INDEX idx_inventory_order
    ON inventory_transactions(order_id);

CREATE INDEX idx_inventory_reference
    ON inventory_transactions(reference_number);

ALTER TABLE inventory_transactions
    ADD CONSTRAINT fk_inventory_created_by
        FOREIGN KEY (created_by) REFERENCES users(id);

-- Tạo bản ghi tồn kho ban đầu cho sản phẩm đang có.
INSERT INTO inventory_transactions (
    product_id,
    order_id,
    transaction_type,
    quantity_changed,
    balance_after,
    reference_number,
    notes,
    created_by,
    created_at
)
SELECT
    product.id,
    NULL,
    'INITIAL_STOCK',
    product.quantity,
    product.quantity,
    CONCAT('MIGRATION-V3-', product.id),
    'Initial inventory snapshot created by Flyway V3',
    NULL,
    CURRENT_TIMESTAMP
FROM products product
WHERE NOT EXISTS (
    SELECT 1
    FROM inventory_transactions transaction
    WHERE transaction.product_id = product.id
);