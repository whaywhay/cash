package kz.store.cash.model;

import java.math.BigDecimal;

public record ProductDto(
    String categoryRefId,
    String productName,
    String barcode,
    BigDecimal wholesalePrice) {

}
