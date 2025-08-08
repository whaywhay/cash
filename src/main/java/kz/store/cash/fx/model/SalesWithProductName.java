package kz.store.cash.fx.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public record SalesWithProductName(
    Long id,
    String barcode,
    String productName,
    BigDecimal soldPrice,
    BigDecimal originalPrice,
    BigDecimal wholesalePrice,
    int quantity,
    BigDecimal total,
    boolean returnFlag,
    LocalDateTime returnDate
) {

}
