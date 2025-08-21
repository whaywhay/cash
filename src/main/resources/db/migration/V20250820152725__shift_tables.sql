create table if not exists main.user
(
    id           bigint generated always as identity
        primary key,
    created      timestamp        default now() not null,
    created_by   text    not null,
    last_upd     timestamp        default now() not null,
    last_upd_by  text    not null,
    username     text    not null unique,
    password     text    not null,
    display_name text    not null,
    role         text    not null default 'CASHIER',
    active       boolean not null default true
);
COMMENT ON COLUMN main.user.username IS 'Fill login names';
COMMENT ON COLUMN main.user.password IS 'Fill pass hashed for logins';
COMMENT ON COLUMN main.user.display_name IS 'Fill fill names';
COMMENT ON COLUMN main.user.role IS 'Role type: CASHIER or ADMIN';
COMMENT ON COLUMN main.user.active IS 'true - user is valid, false - user is not valid';

create table if not exists main.cash_shift
(
    id                  bigint generated always as identity
        primary key,
    created             timestamp default now() not null,
    created_by          text                    not null,
    last_upd            timestamp default now() not null,
    last_upd_by         text                    not null,
    status              text                    not null,
    shift_opened_date   timestamp               not null default now(),
    opened_user_id      bigint                  not null references main.user (id),
    cash_during_opening numeric(15, 2)          not null,
    shift_closed_date   timestamp,
    closed_by_id        bigint references main.user (id),
    sum_cash            numeric(15, 2),
    sum_card            numeric(15, 2),
    left_in_drawer      numeric(15, 2),
    note                text
);
create index if not exists cash_shift_x1 on main.cash_shift (created desc, status);
create index if not exists cash_shift_x2 on main.cash_shift (shift_closed_date desc, status);

COMMENT ON COLUMN main.cash_shift.status IS 'status - OPEN && CLOSED';
COMMENT ON COLUMN main.cash_shift.shift_opened_date IS 'shift opened date time';
COMMENT ON COLUMN main.cash_shift.opened_user_id IS 'opened user id';
COMMENT ON COLUMN main.cash_shift.cash_during_opening IS 'cash which deposited during shift opening';
COMMENT ON COLUMN main.cash_shift.shift_closed_date IS 'shift closed date time';
COMMENT ON COLUMN main.cash_shift.closed_by_id IS 'closed user id';
COMMENT ON COLUMN main.cash_shift.sum_cash IS 'про суммированная наличная сумма во время закрытия';
COMMENT ON COLUMN main.cash_shift.sum_card IS 'про суммированная безналичная сумма во время закрытия';
COMMENT ON COLUMN main.cash_shift.left_in_drawer IS 'оставленная сумма в кассе во время закрытия смены';
COMMENT ON COLUMN main.cash_shift.note IS 'Сюда можно писать комментарии';

create table if not exists main.cash_movement
(
    id            bigint generated always as identity
        primary key,
    created       timestamp default now() not null,
    created_by    text                    not null,
    last_upd      timestamp default now() not null,
    last_upd_by   text                    not null,
    cash_shift_id bigint                  not null references main.cash_shift (id) on delete cascade,
    type          text                    not null, -- IN / OUT
    amount        numeric(15, 2)          not null check (amount > 0),
    reason        text,
    created_by_id bigint                  not null references main.user (id)
);
create index if not exists cash_movement_x1 on main.cash_movement (cash_shift_id);
COMMENT ON COLUMN main.cash_movement.cash_shift_id IS 'foreign key to table cash_shift.id';
COMMENT ON COLUMN main.cash_movement.type IS 'action type - IN means deposited cash, OUT means withdraw cash';
COMMENT ON COLUMN main.cash_movement.amount IS 'cash sum';
COMMENT ON COLUMN main.cash_movement.reason IS 'You can fill reason text';
COMMENT ON COLUMN main.cash_movement.created_by_id IS 'which one who do action';

alter table if exists main.payment_receipt
    add column if not exists cash_shift_id bigint references main.cash_shift (id);
create index if not exists payment_receipt_x3 on main.payment_receipt (cash_shift_id);
COMMENT ON COLUMN main.payment_receipt.cash_shift_id IS 'foreign key to table cash_shift.id';