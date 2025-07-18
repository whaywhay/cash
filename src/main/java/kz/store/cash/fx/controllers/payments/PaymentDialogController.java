package kz.store.cash.fx.controllers.payments;

import java.io.IOException;
import java.util.function.UnaryOperator;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import kz.store.cash.fx.model.PaymentSumDetails;
import kz.store.cash.util.UtilNumbers;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Getter
public class PaymentDialogController {

  @FXML
  public DialogPane paymentWindow;
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

  private boolean paymentConfirmed = false;
  private CashViewController cashViewController;
  private CardViewController cardViewController;
  private MixedViewController mixedViewController;

  private PaymentSumDetails paymentSumDetails;

  private StringBuilder currentInput = new StringBuilder();

  public void initData() {
    ToggleGroup group = new ToggleGroup();
    cashButton.setToggleGroup(group);
    cardButton.setToggleGroup(group);
    mixedButton.setToggleGroup(group);
    cashButton.setSelected(true);
    switchToCash();
  }

  @FXML
  public void switchToCash() {
    if (cashViewController == null) {
      cashViewController = loadCashView();
      setupDecimalFilter(cashViewController.cashAmountField);
    }
    modeContent.getChildren().setAll(cashViewController.getRoot());
    clearAllInputs();
  }

  @FXML
  public void switchToCard() {
    if (cardViewController == null) {
      cardViewController = loadCardView();
      setupDecimalFilter(cardViewController.cardAmountField);
    }
    modeContent.getChildren().setAll(cardViewController.getRoot());
    clearAllInputs();
    log.info("after switchToCard clearAllInputs");
    double total = paymentSumDetails.getTotalToPay();
    cardViewController.cardAmountField.setText(String.valueOf(total));
    paymentSumDetails.setReceivedPayment(total);
    updateValues();
    //currentInput.append(total);
  }

  @FXML
  public void switchToMixed() {
    if (mixedViewController == null) {
      mixedViewController = loadMixedView();
      setupDecimalFilter(mixedViewController.mixedCashField);
      setupDecimalFilter(mixedViewController.mixedCardField);
    }
    modeContent.getChildren().setAll(mixedViewController.getRoot());
    clearAllInputs();
  }

  private CashViewController loadCashView() {
    return loadView("/fxml/payment/cash_view.fxml", CashViewController.class);
  }

  private CardViewController loadCardView() {
    return loadView("/fxml/payment/card_view.fxml", CardViewController.class);
  }

  private MixedViewController loadMixedView() {
    return loadView("/fxml/payment/mixed_view.fxml", MixedViewController.class);
  }

  private <T> T loadView(String fxmlPath, Class<T> controllerClass) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
      Parent root = loader.load();
      T controller = loader.getController();
      if (controller instanceof HasRoot viewWithRoot) {
        viewWithRoot.setRoot(root);
      }
      return controller;
    } catch (IOException e) {
      throw new RuntimeException("Ошибка при загрузке: " + fxmlPath, e);
    }
  }

  private void clearAllInputs() {
    paymentSumDetails.setReceivedPayment(0);
    currentInput.setLength(0);
    if (cashViewController != null) {
      cashViewController.cashAmountField.clear();
    }
    if (cardViewController != null) {
      cardViewController.cardAmountField.clear();
    }
    if (mixedViewController != null) {
      mixedViewController.mixedCashField.clear();
      mixedViewController.mixedCardField.clear();
    }
    updateValues();
  }

  public void setPaymentDetailsSum(PaymentSumDetails paymentSumDetails) {
    this.paymentSumDetails = paymentSumDetails;
    initData();
  }


  @FXML
  private void onDigitPress(ActionEvent event) {
    String digit = ((Button) event.getSource()).getText();
    if (digit.equals(".") && currentInput.toString().contains(".")) {
      return;
    }
    currentInput.append(digit);
    updateFromInput();
  }

  private void updatePaymentTextField() {
    if (cashViewController != null) {
      cashViewController.cashAmountField.setText(currentInput.toString());
    }
    if (cardViewController != null) {
      cardViewController.cardAmountField.setText(currentInput.toString());
    }
    if (mixedViewController != null) {
      mixedViewController.mixedCashField.setText(currentInput.toString());
      var remainingForCardPayment =
          paymentSumDetails.getTotalToPay() - paymentSumDetails.getReceivedPayment();
      if (remainingForCardPayment > 0) {
        mixedViewController.mixedCardField.setText(String.valueOf(remainingForCardPayment));
      } else {
        mixedViewController.mixedCardField.setText(String.valueOf(0));
      }
    }
  }

  private boolean checkPaymentSumEnough() {
    if (mixedButton.isSelected()) {
      log.info("mixedButton.isSelected(): ");
      double cash = UtilNumbers.parseDoubleAmount(mixedViewController.mixedCardField.getText());
      double card = UtilNumbers.parseDoubleAmount(mixedViewController.mixedCardField.getText());
      return (cash + card) >= paymentSumDetails.getTotalToPay();
    }
    log.info("cash or card toggle: ");
    return paymentSumDetails.getTotalToPay() >= paymentSumDetails.getReceivedPayment();
  }


  @FXML
  private void onBackspace() {
    if (!currentInput.isEmpty()) {
      currentInput.deleteCharAt(currentInput.length() - 1);
      updateFromInput();
    }
  }

  @FXML
  private void onAmountAdd(ActionEvent event) {
    Button btn = (Button) event.getSource();
    String text = btn.getText().replace("+", "").trim();
    double buttonSum = UtilNumbers.parseDoubleAmount(text);
    double result = UtilNumbers.parseDoubleAmount(currentInput.toString()) + buttonSum;
    currentInput.setLength(0);
    currentInput.append(result);
    updateFromInput();
  }

  @FXML
  private void onPay() {
    if (checkPaymentSumEnough()) {
      return;
    }
    paymentConfirmed = true;
    Stage stage = (Stage) paymentWindow.getScene().getWindow();
    stage.close();
  }

  @FXML
  private void onCancel() {
    paymentConfirmed = false;
    Stage stage = (Stage) paymentWindow.getScene().getWindow();
    stage.close();
  }

  private void updateFromInput() {
    try {
      double receivedAmount = UtilNumbers.parseDoubleAmount(currentInput.toString());
      paymentSumDetails.setReceivedPayment(receivedAmount);
      updatePaymentTextField();
    } catch (NumberFormatException e) {
      paymentSumDetails.setReceivedPayment(0);
    }
    updateValues();
  }

  private void updateValues() {
    totalLabel.setText(String.format("%.2f тг", paymentSumDetails.getTotalToPay()));
    receivedPaymentLabel.setText(String.format("%.2f тг", paymentSumDetails.getReceivedPayment()));
    remainingPaymentLabel.setText(
        String.format("%.2f тг", paymentSumDetails.getRemainingPayment()));
    changeMoneyLabel.setText(String.format("%.2f тг", paymentSumDetails.getChangeMoney()));
  }

  private void setupDecimalFilter(TextField textField) {
    UnaryOperator<TextFormatter.Change> filter = change -> {
      String newText = change.getControlNewText();
      if (newText.matches("\\d*(\\.\\d{0,2})?")) {
        return change;
      } else {
        return null;
      }
    };
    textField.setTextFormatter(new TextFormatter<>(filter));
  }


}