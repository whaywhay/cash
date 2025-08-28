ALTER TABLE main.payment_receipt
RENAME COLUMN non_cash_payment TO card_payment;

ALTER TABLE main.payment_receipt
ADD COLUMN if not exists change_money numeric(15, 2) default 0;

ALTER TABLE main.payment_receipt
    ADD COLUMN if not exists remaining_payment numeric(15, 2) default 0;

ALTER TABLE main.payment_receipt
    ADD COLUMN if not exists received_payment numeric(15, 2) default 0;

ALTER TABLE main.payment_receipt
    ADD COLUMN if not exists total numeric(15, 2) default 0;

ALTER TABLE main.payment_receipt
    ALTER COLUMN cash_payment SET DEFAULT 0;

ALTER TABLE main.payment_receipt
    ALTER COLUMN card_payment SET DEFAULT 0;

ALTER TABLE main.payment_receipt
    DROP COLUMN if exists status;

ALTER TABLE main.payment_receipt
    ADD COLUMN if not exists status text not null ;

