ALTER TABLE main.category
    DROP COLUMN IF EXISTS full_path;

CREATE UNIQUE INDEX category_u1
    ON main.category (category_code);
