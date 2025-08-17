package kz.store.cash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ProductDto(
    @JsonProperty("category_ref_id") String categoryRefId,
    @JsonProperty("product_name") String productName,
    @JsonProperty("barcode") String barcode,
    @JsonProperty("wholesale_price") BigDecimal wholesalePrice) {

}
