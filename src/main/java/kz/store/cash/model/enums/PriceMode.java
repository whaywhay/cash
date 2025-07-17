package kz.store.cash.model.enums;

import lombok.Getter;

@Getter
public enum PriceMode {
  ORIGINAL("ЦЕНА"),
  WHOLESALE("ОПТОВАЯ ЦЕНА");

  private final String displayName;

  PriceMode(String displayName) {
    this.displayName = displayName;
  }

}
