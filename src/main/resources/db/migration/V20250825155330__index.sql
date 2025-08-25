drop index cash_store.main.cash_shift_only_one_open;
create unique index if not exists cash_shift_x4
    on main.cash_shift ((1))
    where status = 'OPENED';