package kz.store.cash.fx.model;

import kz.store.cash.model.enums.PaymentType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Setter
@Getter
@ToString
public class PaymentSumDetails {

  private double totalToPay;
  private double receivedPayment;
  private double remainingPayment;
  private double changeMoney;
  private PaymentType paymentType;
  private double cashPayment;
  private double cardPayment;

  public PaymentSumDetails(double totalToPay, double receivedPayment) {
    this.totalToPay = totalToPay;
    this.receivedPayment = receivedPayment;
    this.remainingPayment =
        (totalToPay > receivedPayment) ? totalToPay - receivedPayment : 0;
    this.changeMoney = receivedPayment > totalToPay ? receivedPayment - totalToPay: 0;
  }

  public void setCalculate(double totalToPay, double receivedPayment) {
    this.totalToPay = totalToPay;
    this.receivedPayment = receivedPayment;
    this.remainingPayment =
        (totalToPay > receivedPayment) ? totalToPay - receivedPayment : 0;
    this.changeMoney = receivedPayment > totalToPay ? receivedPayment - totalToPay: 0;
  }

  public void setReceivedPayment(double receivedPayment) {
    this.receivedPayment = receivedPayment;
    setCalculate(totalToPay, receivedPayment);
  }
}
