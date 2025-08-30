ALTER TABLE main.app_settings
    ADD COLUMN IF NOT EXISTS category_web_address text,
    ADD COLUMN IF NOT EXISTS product_web_address  text,
    ADD COLUMN IF NOT EXISTS login                text,
    ADD COLUMN IF NOT EXISTS password             text;