DROP INDEX cash_store.main.cash_shift_x3;
CREATE INDEX cash_shift_x3
    ON main.cash_shift (status, shift_opened_date DESC);