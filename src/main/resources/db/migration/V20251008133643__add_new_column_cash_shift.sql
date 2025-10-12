ALTER TABLE main.cash_shift
    ADD COLUMN IF NOT EXISTS sum_debt numeric(15, 2);