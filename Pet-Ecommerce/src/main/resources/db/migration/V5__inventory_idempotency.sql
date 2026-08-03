ALTER TABLE inventory_transactions
    ADD COLUMN idempotency_key VARCHAR(180) NULL;

CREATE UNIQUE INDEX uk_inventory_idempotency
    ON inventory_transactions(idempotency_key);
