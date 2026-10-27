-- Полная схема Cash POS для SQLite.
--
-- Это не построчный перевод старых Postgres-миграций: там было 23 инкрементальных шага
-- (в т.ч. добавить колонку -> потом удалить, переименовать и т.п.), что на новой базе просто
-- бессмысленно повторять. Здесь сразу целевое состояние схемы.
--
-- Без схемы вида Postgres "main.table" — в SQLite один файл базы и один неймспейс таблиц,
-- разделять их по схемам незачем (а внутри REFERENCES SQLite вообще не принимает
-- schema.table — только имя таблицы).

create table category
(
    id                  integer primary key autoincrement,
    created             timestamp not null,
    created_by          text      not null,
    last_upd            timestamp not null,
    last_upd_by         text      not null,
    category_code       text      not null,
    category_name       text      not null,
    parent_category_id  text
);
create unique index category_u1 on category (category_code);

create table product
(
    id              integer primary key autoincrement,
    created         timestamp      not null,
    created_by      text           not null,
    last_upd        timestamp      not null,
    last_upd_by     text           not null,
    barcode         text           not null,
    product_name    text           not null,
    original_price  numeric(15, 2) not null default 0,
    wholesale_price numeric(15, 2) not null,
    category_ref_id integer references category (id) on delete restrict
);
create unique index product_u1 on product (barcode);
create index product_x1 on product (product_name);

create table user
(
    id           integer primary key autoincrement,
    created      timestamp not null,
    created_by   text      not null,
    last_upd     timestamp not null,
    last_upd_by  text      not null,
    username     text      not null unique,
    password     text      not null,
    display_name text      not null,
    role         text      not null default 'CASHIER',
    active       boolean   not null default 1
);

create table cash_shift
(
    id                  integer primary key autoincrement,
    created             timestamp      not null,
    created_by          text           not null,
    last_upd            timestamp      not null,
    last_upd_by         text           not null,
    status              text           not null,
    shift_opened_date   timestamp      not null,
    opened_user_id      integer        not null references user (id),
    cash_during_opening numeric(15, 2) not null,
    shift_closed_date   timestamp,
    closed_by_id        integer references user (id),
    sum_cash            numeric(15, 2),
    sum_card            numeric(15, 2),
    left_in_drawer      numeric(15, 2),
    note                text,
    sum_return_cash     numeric(15, 2),
    sum_return_card     numeric(15, 2),
    sum_debt            numeric(15, 2),
    sum_debt_return     numeric(15, 2)
);
create index cash_shift_x1 on cash_shift (created desc, status);
create index cash_shift_x2 on cash_shift (shift_closed_date desc, status);
create index cash_shift_x3 on cash_shift (status, shift_opened_date desc);
-- Не более одной открытой смены одновременно: индексируем status только у строк со
-- status='OPENED' — раз все включённые в индекс строки равны 'OPENED', уникальность
-- допускает максимум одну такую строку.
create unique index cash_shift_only_one_open on cash_shift (status) where status = 'OPENED';

create table cash_movement
(
    id            integer primary key autoincrement,
    created       timestamp      not null,
    created_by    text           not null,
    last_upd      timestamp      not null,
    last_upd_by   text           not null,
    cash_shift_id integer        not null references cash_shift (id) on delete cascade,
    type          text           not null, -- IN / OUT
    amount        numeric(15, 2) not null check (amount > 0),
    reason        text,
    created_by_id integer        not null references user (id)
);
create index cash_movement_x1 on cash_movement (cash_shift_id);

create table payment_receipt
(
    id                   integer primary key autoincrement,
    created              timestamp      not null,
    created_by           text           not null,
    last_upd             timestamp      not null,
    last_upd_by          text           not null,
    return_date          timestamp,
    payment_type         text,
    cash_payment         numeric(15, 2) default 0,
    card_payment         numeric(15, 2) default 0,
    status               text           not null,
    change_money         numeric(15, 2) default 0,
    remaining_payment    numeric(15, 2) default 0,
    received_payment     numeric(15, 2) default 0,
    total                numeric(15, 2) default 0,
    diary_debt_customer_id text,
    original_receipt_id  integer references payment_receipt (id) on delete set null,
    cash_shift_id        integer references cash_shift (id)
);
create index payment_receipt_x1 on payment_receipt (original_receipt_id);
create index payment_receipt_x2 on payment_receipt (created);
create index payment_receipt_x3 on payment_receipt (cash_shift_id);

create table sales
(
    id                 integer primary key autoincrement,
    created            timestamp      not null,
    created_by         text           not null,
    last_upd           timestamp      not null,
    last_upd_by        text           not null,
    barcode            text           not null,
    sold_price         numeric(15, 2) not null,
    original_price     numeric(15, 2) not null,
    wholesale_price    numeric(15, 2) not null,
    quantity           int            not null,
    total              numeric(15, 2) not null,
    payment_receipt_id integer references payment_receipt (id) on delete cascade,
    return_flag        boolean        not null default 0,
    return_date        timestamp
);
create index sales_x1 on sales (payment_receipt_id);
create index sales_x3 on sales (created);

create table quick_product
(
    id          integer primary key autoincrement,
    created     timestamp not null,
    created_by  text      not null,
    last_upd    timestamp not null,
    last_upd_by text      not null,
    barcode     text      not null unique,
    sorted_date timestamp not null
);
create index quick_product_x1 on quick_product (sorted_date desc);

create table app_settings
(
    id                       integer primary key autoincrement,
    created                  timestamp not null,
    created_by               text      not null,
    last_upd                 timestamp not null,
    last_upd_by              text      not null,
    org_name                 text      not null,
    bin                      text,
    address                  text,
    sale_store               text      not null,
    category_web_address     text,
    product_web_address      text,
    login                    text,
    password                 text,
    debt_diary_base_address  text,
    debt_diary_login         text,
    debt_diary_password      text
);
