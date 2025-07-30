package kz.store.cash.model.enums;

import lombok.Getter;

@Getter
public enum PaymentType {
  CASH("Наличные"),
  CARD("Банковский счет"),
  MIXED("Смешанная оплата");

  private final String displayName;

  PaymentType(String name) {
    this.displayName = name;
  }

}
