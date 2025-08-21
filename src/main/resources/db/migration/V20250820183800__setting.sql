create table main.app_settings
(
    id          bigint generated always as identity
        primary key,
    created     timestamp            default now() not null,
    created_by  text        not null,
    last_upd    timestamp            default now() not null,
    last_upd_by text        not null,
    org_name    text        not null,
    bin         text,
    address     text,
    sale_store  text        not null
);