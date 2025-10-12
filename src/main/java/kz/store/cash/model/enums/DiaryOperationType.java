package kz.store.cash.model.enums;

import lombok.Getter;

@Getter
public enum DiaryOperationType {
  DEBT_SALE("Реализация(Долг)", "/api/types/2"),
  RETURN("Возврат","/api/types/3");

  private final String displayName;
  private final String type;
  DiaryOperationType(String displayName, String type) {
    this.displayName = displayName;
    this.type = type;
  }
}
