ALTER TABLE main.sales
    ALTER COLUMN return_flag TYPE boolean
        USING return_flag::boolean