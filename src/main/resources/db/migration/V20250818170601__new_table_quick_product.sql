create table if not exists main.quick_product
(
    id          bigint generated always as identity primary key,
    created     timestamp default now() not null,
    created_by  text                    not null,
    last_upd    timestamp default now() not null,
    last_upd_by text                    not null,
    barcode     text                    not null unique,
    sorted_date timestamp default now() not null
);

create index if not exists quick_product_u1 on main.quick_product (sorted_date desc);