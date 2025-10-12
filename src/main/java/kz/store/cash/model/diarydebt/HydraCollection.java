package kz.store.cash.model.diarydebt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HydraCollection<T> {

  @JsonProperty("hydra:member")
  public List<T> member;
  @JsonProperty("hydra:totalItems")
  public Integer totalItems;
  @JsonProperty("hydra:view")
  public HydraView view;
}
