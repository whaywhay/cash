ALTER TABLE main.app_settings
    ADD COLUMN IF NOT EXISTS debt_diary_base_address text,
    ADD COLUMN IF NOT EXISTS debt_diary_login  text,
    ADD COLUMN IF NOT EXISTS debt_diary_password text;