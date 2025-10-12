package kz.store.cash.model.diarydebt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HydraView {

  @JsonProperty("@id")
  public String self;
  @JsonProperty("hydra:first")
  public String first;
  @JsonProperty("hydra:last")
  public String last;
  @JsonProperty("hydra:next")
  public String next;
  @JsonProperty("hydra:previous")
  public String previous;
}