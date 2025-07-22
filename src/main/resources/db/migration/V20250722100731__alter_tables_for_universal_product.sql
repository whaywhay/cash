ALTER TABLE main.payment_receipt
    ALTER COLUMN payment_type DROP NOT NULL;

ALTER TABLE main.payment_receipt
    ADD COLUMN status text not null;

ALTER TABLE main.sales
    ADD COLUMN return_flag text not null default false;
ALTER TABLE main.sales
    ADD COLUMN return_date timestamp;

INSERT INTO main.product (created_by,
                     last_upd_by,
                     barcode,
                     product_name,
                     original_price,
                     wholesale_price,
                     category_ref_id)
VALUES ('system',
        'system',
        'universal-product-200001', -- штрихкод с контрольной цифрой
        'Универсальный продукт',
        0,
        0,
        NULL);

