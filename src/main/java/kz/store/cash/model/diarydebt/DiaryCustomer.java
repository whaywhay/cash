package kz.store.cash.model.diarydebt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DiaryCustomer(
    Long id,
    String name,
    String place,
    String contact,
    MarketRef market,
    String total,
    @JsonProperty("lastTransactionAt") String lastTransactionAt
) {

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record MarketRef(
      @JsonProperty("@id") String iri,
      @JsonProperty("@type") String type,
      @JsonProperty("id") Long numericId,
      @JsonProperty("title") String title

  ) {

  }
}


