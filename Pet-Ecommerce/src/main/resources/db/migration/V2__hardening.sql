-- Hardening schema & thêm tính năng mới

ALTER TABLE products
    MODIFY COLUMN description TEXT NOT NULL;

ALTER TABLE carts
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE cart_items
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE orders
    ADD COLUMN idempotency_key VARCHAR(64) NULL;

CREATE UNIQUE INDEX uk_order_user_idempotency
    ON orders(user_id, idempotency_key);

-- Bảng refund_requests (sau khi tạo entity)
CREATE TABLE refund_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL UNIQUE,
    requested_by BIGINT NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider_refund_ref VARCHAR(100),
    admin_note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (requested_by) REFERENCES users(id),
    CONSTRAINT uk_refund_order UNIQUE (order_id)
);

-- Bảng audit_logs
CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_user_id BIGINT NOT NULL,
    actor_email VARCHAR(255) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    request_path TEXT NOT NULL,
    response_status INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_actor_user_id (actor_user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_request_path (request_path(100))
);

-- Bảng inventory_transactions
CREATE TABLE inventory_transactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    order_id BIGINT,
    transaction_type VARCHAR(20) NOT NULL,
    quantity_changed INT NOT NULL,
    reference_number VARCHAR(100),
    notes TEXT,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_product (product_id),
    INDEX idx_created_at (created_at)
);