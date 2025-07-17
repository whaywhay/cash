package kz.store.cash.fx.controllers.payments;

import java.io.IOException;
import java.util.Objects;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import kz.store.cash.fx.model.PaymentSumDetails;
import kz.store.cash.util.ParseNumbers;

public class PaymentDialogController {

  @FXML
  private ToggleButton cashButton;
  @FXML
  private ToggleButton cardButton;
  @FXML
  private ToggleButton mixedButton;

  @FXML
  public Label totalLabel;
  @FXML
  public Label receivedPaymentLabel;
  @FXML
  private Label remainingPaymentLabel;
  @FXML
  private Label changeMoneyLabel;
  @FXML
  private StackPane modeContent;

  private CashViewController cashViewController;
  private CardViewController cardViewController;
  private MixedViewController mixedViewController;

  private PaymentSumDetails paymentSumDetails;

  private final StringBuilder currentInput = new StringBuilder();

  public void initData() {
    ToggleGroup group = new ToggleGroup();
    cashButton.setToggleGroup(group);
    cardButton.setToggleGroup(group);
    mixedButton.setToggleGroup(group);

    //totalToPay = parseAmount(totalLabel.getText());
    //updateValues();

    // Default mode
    cashButton.setSelected(true);
    switchToCash();
  }

  @FXML
  private void switchToCash() {
    clearInput();
    loadModeView("/fxml/payment/cash_view.fxml");
  }

  @FXML
  private void switchToCard() {
    clearInput();
    paymentSumDetails.setReceivedPayment(paymentSumDetails.getTotalToPay());
    updateValues();
    loadModeView("/fxml/payment/card_view.fxml");
  }

  @FXML
  private void switchToMixed() {
    clearInput();
    loadModeView("/fxml/payment/mixed_view.fxml");
  }

  private void loadModeView(String fxmlPath) {
    try {
      Node view = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxmlPath)));
      modeContent.getChildren().setAll(view);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void setPaymentDetailsSum(PaymentSumDetails paymentSumDetails) {
    this.paymentSumDetails = paymentSumDetails;
    initData();
  }

  public PaymentSumDetails getPaymentDetailsSum() {
    return paymentSumDetails;
  }


  @FXML
  private void onDigitPress(ActionEvent event) {
    Button source = (Button) event.getSource();
    currentInput.append(source.getText());
    updateFromInput();
  }

  @FXML
  private void onBackspace() {
    if (!currentInput.isEmpty()) {
      currentInput.deleteCharAt(currentInput.length() - 1);
      updateFromInput();
    }
  }

  @FXML
  private void onClear() {
    clearInput();
  }

  @FXML
  private void onAmountAdd(ActionEvent event) {
//    Button btn = (Button) event.getSource();
//    String text = btn.getText().replace("+", "").trim();
//    long add = parseAmount(text);
//    receivedAmount += add;
//    updateValues();
  }

  @FXML
  private void onPay() {
    // передача данных в SalesController и закрытие окна
    Stage stage = (Stage) totalLabel.getScene().getWindow();
    stage.close();
  }

  @FXML
  private void onCancel() {
    Stage stage = (Stage) totalLabel.getScene().getWindow();
    stage.close();
  }

  private void updateFromInput() {
    try {
      double receivedAmount = ParseNumbers.parseDoubleAmount(currentInput.toString());
      paymentSumDetails.setReceivedPayment(receivedAmount);
    } catch (NumberFormatException e) {
      paymentSumDetails.setReceivedPayment(0);
    }
    updateValues();
  }

  private void clearInput() {
    currentInput.setLength(0);
    paymentSumDetails.setReceivedPayment(0);
    updateValues();
  }

  private void updateValues() {
    totalLabel.setText(String.format("%.2f тг", paymentSumDetails.getTotalToPay()));
    receivedPaymentLabel.setText(String.format("%.2f тг", paymentSumDetails.getReceivedPayment()));
    remainingPaymentLabel.setText(
        String.format("%.2f тг", paymentSumDetails.getRemainingPayment()));
    changeMoneyLabel.setText(String.format("%.2f тг", paymentSumDetails.getChangeMoney()));

  }
}