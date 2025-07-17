package kz.store.cash.fx.model;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class PaymentSumDetails {

  private double totalToPay;
  private double receivedPayment;
  private double remainingPayment;
  private double changeMoney;

  public PaymentSumDetails(double totalToPay, double receivedPayment) {
    this.totalToPay = totalToPay;
    this.receivedPayment = receivedPayment;
    this.remainingPayment =
        (totalToPay > receivedPayment) ? totalToPay - receivedPayment : 0;
    this.changeMoney = receivedPayment > totalToPay ? receivedPayment - totalToPay: 0;
  }

  public void setTotalToPay(double totalToPay) {
    this.totalToPay = totalToPay;
    receivedPayment = 0;
    remainingPayment = 0;
    changeMoney = 0;
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
