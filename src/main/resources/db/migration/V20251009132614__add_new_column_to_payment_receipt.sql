ALTER TABLE main.payment_receipt
    ADD COLUMN IF NOT EXISTS diary_debt_customer_id text;