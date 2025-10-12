ALTER TABLE main.cash_shift
    ADD COLUMN IF NOT EXISTS sum_debt_return numeric(15, 2);