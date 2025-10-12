package kz.store.cash.model.enums;

import lombok.Getter;

@Getter
public enum PaymentType {
  CASH("Наличные"),
  CARD("Банковский счет"),
  MIXED("Смешанная оплата"),
  DEBT("В долг");

  private final String displayName;

  PaymentType(String name) {
    this.displayName = name;
  }

}
