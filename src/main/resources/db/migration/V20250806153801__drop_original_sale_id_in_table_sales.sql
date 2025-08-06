ALTER TABLE main.sales
    DROP CONSTRAINT IF EXISTS sales_original_sale_id_fkey;
ALTER TABLE main.sales
    DROP COLUMN IF EXISTS original_sale_id;
DROP INDEX IF EXISTS sales_x2;