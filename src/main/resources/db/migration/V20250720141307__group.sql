create table if not exists main.category
(
    id                 bigint generated always as identity primary key,
    created            timestamp default now() not null,
    created_by         text                    not null,
    last_upd           timestamp default now() not null,
    last_upd_by        text                    not null,
    category_code      text                    not null,
    category_name      text                    not null,
    full_path          text                    not null,
    parent_category_id text
);
create unique index if not exists group_u1
    on main.category (category_code);

create index if not exists group_x1
    on main.category (full_path);

alter table main.product
    add column category_ref_id bigint;

ALTER TABLE main.product
    ADD CONSTRAINT fk_product_group
        FOREIGN KEY (category_ref_id)
            REFERENCES main.category (id)
            ON DELETE RESTRICT;