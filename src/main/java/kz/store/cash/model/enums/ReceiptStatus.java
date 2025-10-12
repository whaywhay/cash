package kz.store.cash.model.enums;

import lombok.Getter;

@Getter
public enum ReceiptStatus {
  SALE("Продажа"),
  RETURN("Возврат"),
  RETURN_NO_RECEIPT("Возв. без чека"),
  PENDING("Отложенный чек"),
  DEBT_RECEIPT("Долговой чек"),
  DEBT_RECEIPT_RETURN("Возв. долг. чек");

  private final String displayName;

  ReceiptStatus(String name) {
    this.displayName = name;
  }
}
