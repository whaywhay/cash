create table if not exists main.product
(
    id              bigint generated always as identity primary key,
    created         timestamp      default now() not null,
    created_by      text                         not null,
    last_upd        timestamp      default now() not null,
    last_upd_by     text                         not null,
    barcode         text                         not null,
    product_name    text                         not null,
    original_price  numeric(15, 2) default 0     not null,
    wholesale_price numeric(15, 2)               not null
);
create unique index if not exists product_u1
    on main.product (barcode);

create index if not exists product_x1
    on main.product (product_name);
create table if not exists main.payment_receipt
(
    id               bigint generated always as identity primary key,
    created          timestamp default now() not null,
    created_by       text                    not null,
    last_upd         timestamp default now() not null,
    last_upd_by      text                    not null,
    return_flag      boolean                 not null default false,
    return_date      timestamp,
    payment_type     text                    not null,
    cash_payment     numeric(15, 2),
    non_cash_payment numeric(15, 2)
);

create table if not exists main.sales
(
    id                 bigint generated always as identity primary key,
    created            timestamp default now() not null,
    created_by         text                    not null,
    last_upd           timestamp default now() not null,
    last_upd_by        text                    not null,
    barcode            text                    not null,
    sold_price         numeric(15, 2)          not null,
    original_price     numeric(15, 2)          not null,
    wholesale_price    numeric(15, 2)          not null,
    quantity           int                     not null,
    total              numeric(15, 2)          not null,
    payment_receipt_id bigint references main.payment_receipt (id)
);
create index if not exists sales_x1 on main.sales (payment_receipt_id);

