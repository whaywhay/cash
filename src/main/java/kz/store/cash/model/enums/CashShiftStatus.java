package kz.store.cash.model.enums;

import lombok.Getter;

@Getter
public enum CashShiftStatus {
  OPEN("СМЕНА ОТКРЫТА"),
  CLOSE("СМЕНА ЗАКРЫТА");

  private final String displayName;

  CashShiftStatus(String name) {
    this.displayName = name;
  }
}
