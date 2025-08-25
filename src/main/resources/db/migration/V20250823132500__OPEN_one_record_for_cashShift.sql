create unique index if not exists cash_shift_only_one_open
    on main.cash_shift ((1))
    where status = 'OPEN';