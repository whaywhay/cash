create index if not exists cash_shift_x3
    on main.cash_shift (shift_opened_date desc, status);