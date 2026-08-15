ALTER TABLE reviews
    ADD COLUMN admin_reply TEXT NULL,
    ADD COLUMN replied_by_name VARCHAR(255) NULL,
    ADD COLUMN replied_at DATETIME(6) NULL;
