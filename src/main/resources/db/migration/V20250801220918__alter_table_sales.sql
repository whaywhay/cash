ALTER TABLE main.payment_receipt
    ADD COLUMN original_receipt_id BIGINT REFERENCES main.payment_receipt (id) ON DELETE SET NULL;
CREATE INDEX payment_receipt_x1 ON main.payment_receipt (original_receipt_id);
CREATE INDEX payment_receipt_x2 ON main.payment_receipt (created);

ALTER TABLE main.sales
    ADD COLUMN original_sale_id BIGINT REFERENCES main.sales (id) ON DELETE SET NULL;

ALTER TABLE main.sales
    DROP CONSTRAINT IF EXISTS sales_payment_receipt_id_fkey;

ALTER TABLE main.sales
    ADD CONSTRAINT fk_sales_payment_receipt
        FOREIGN KEY (payment_receipt_id)
            REFERENCES main.payment_receipt (id)
            ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS sales_x2
    ON main.sales (original_sale_id);

CREATE INDEX IF NOT EXISTS sales_x3
    ON main.sales (created);