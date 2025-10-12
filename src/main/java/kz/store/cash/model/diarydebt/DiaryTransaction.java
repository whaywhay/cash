package kz.store.cash.model.diarydebt;

import kz.store.cash.model.enums.DiaryOperationType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DiaryTransaction {

  int amount;
  String type;
  String customer;
  String comment;

  public DiaryTransaction(String customer, int amount) {
    this.amount = amount;
    if (!customer.startsWith("/api/customers/")) {
      this.customer = "/api/customers/" + customer;
    }else  {
      this.customer = customer;
    }

  }

  public DiaryTransaction modifyTypeAndComment(DiaryOperationType diaryOperationType, String comment) {
    this.type = diaryOperationType.getType();
    this.comment = comment;
    return this;
  }
}
