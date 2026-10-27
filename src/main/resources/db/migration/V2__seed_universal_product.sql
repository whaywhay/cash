-- "Универсальный товар" — используется ProductService.findUniversalProduct() для продажи
-- товаров без штрихкода по произвольной цене (см. app.product.universal-product-barcode).
insert into product (created, created_by, last_upd, last_upd_by, barcode, product_name,
                      original_price, wholesale_price, category_ref_id)
values (current_timestamp, 'system', current_timestamp, 'system',
        'universal-product-200001', 'Универсальный продукт', 0, 0, null);
