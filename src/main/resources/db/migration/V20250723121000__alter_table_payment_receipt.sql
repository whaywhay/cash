ALTER TABLE main.payment_receipt
RENAME COLUMN non_cash_payment TO card_payment;

ALTER TABLE main.payment_receipt
ADD COLUMN change_money numeric(15, 2) default 0;

ALTER TABLE main.payment_receipt
    ADD COLUMN remaining_payment numeric(15, 2) default 0;

ALTER TABLE main.payment_receipt
    ADD COLUMN received_payment numeric(15, 2) default 0;

ALTER TABLE main.payment_receipt
    ADD COLUMN total numeric(15, 2) default 0;

ALTER TABLE main.payment_receipt
    ALTER COLUMN cash_payment SET DEFAULT 0;

ALTER TABLE main.payment_receipt
    ALTER COLUMN card_payment SET DEFAULT 0;

ALTER TABLE main.payment_receipt
    DROP COLUMN status;

ALTER TABLE main.payment_receipt
    ADD COLUMN status text not null ;

