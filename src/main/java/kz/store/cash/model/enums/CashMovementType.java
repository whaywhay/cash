package kz.store.cash.model.enums;

import lombok.Getter;

@Getter
public enum CashMovementType {
  IN("ВНОС СРЕДСТВ"),
  OUT("ВЫНОС СРЕДСТВ");

  private final String displayName;

  CashMovementType(String name) {
    this.displayName = name;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
