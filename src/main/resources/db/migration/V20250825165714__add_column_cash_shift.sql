ALTER TABLE main.cash_shift
    ADD COLUMN IF NOT EXISTS sum_return_cash numeric(15, 2),
    ADD COLUMN IF NOT EXISTS sum_return_card numeric(15, 2);

COMMENT ON COLUMN main.cash_shift.sum_return_cash IS 'про суммированная возвращенная наличная сумма во время закрытия';
COMMENT ON COLUMN main.cash_shift.sum_return_card IS 'про суммированная возвращенная безналичная сумма во время закрытия';