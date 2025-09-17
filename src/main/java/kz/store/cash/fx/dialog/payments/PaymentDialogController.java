package kz.store.cash.fx.dialog.payments;

import java.io.IOException;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import kz.store.cash.fx.dialog.lib.CancellableDialog;
import kz.store.cash.fx.model.PaymentSumDetails;
import kz.store.cash.handler.ValidationException;
import kz.store.cash.model.enums.PaymentType;
import kz.store.cash.util.UtilNumbers;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Getter
@Component
public class PaymentDialogController implements CancellableDialog {

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
  private ChangeListener<String> activeListener;


  private final ApplicationContext context;
  private boolean paymentConfirmed = false;
  private CashViewController cashViewController;
  private CardViewController cardViewController;
  private MixedViewController mixedViewController;
  private TextField activePaymentField;
  private PaymentSumDetails paymentSumDetails;
  private final UtilNumbers utilNumbers;

  private void hookActiveField(TextField field) {
    // delete old or previous listener
    if (activeListener != null && activePaymentField != null) {
      activePaymentField.textProperty().removeListener(activeListener);
    }
    activePaymentField = field;
    activeListener = (obs, o, n) -> {
      if (!cardButton.isSelected()) {
        updateFromInputNew();
      }
    };
    activePaymentField.textProperty().addListener(activeListener);
    Platform.runLater(this::setFocusOnActivePaymentField);
  }

  @FXML
  public void initialize() {
    paymentWindow.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
      if (e.getCode() == KeyCode.ENTER) {
        onPay();
      }
    });
  }

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
      utilNumbers.setupDecimalFilter(cashViewController.cashAmountField);
    }
    modeContent.getChildren().setAll(cashViewController.getRoot());
    hookActiveField(cashViewController.cashAmountField);
    clearAllInputs();
  }

  @FXML
  public void switchToCard() {
    if (cardViewController == null) {
      cardViewController = loadCardView();
    }
    modeContent.getChildren().setAll(cardViewController.getRoot());
    clearAllInputs();
    double total = paymentSumDetails.getTotalToPay();
    cardViewController.cardAmountField.setText(String.valueOf(total));
    paymentSumDetails.setReceivedPayment(total);
    updateLabelValues();
//    hookActiveField(cardViewController.cardAmountField, false); // ← ввод с клавы запрещён
  }

  @FXML
  public void switchToMixed() {
    if (mixedViewController == null) {
      mixedViewController = loadMixedView();
      utilNumbers.setupDecimalFilter(mixedViewController.mixedCashField);
    }
    modeContent.getChildren().setAll(mixedViewController.getRoot());
    hookActiveField(mixedViewController.mixedCashField);
    clearAllInputs();
  }

  private CashViewController loadCashView() {
    return loadView("/fxml/sales/payment/cash_view.fxml");
  }

  private CardViewController loadCardView() {
    return loadView("/fxml/sales/payment/card_view.fxml");
  }

  private MixedViewController loadMixedView() {
    return loadView("/fxml/sales/payment/mixed_view.fxml");
  }

  private <T> T loadView(String fxmlPath) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
      loader.setControllerFactory(context::getBean);
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
    activePaymentField.setText("");
    if (mixedButton.isSelected()) {
      mixedViewController.mixedCardField.setText("");
    }
    updateLabelValues();
  }

  public void setPaymentDetailsSum(PaymentSumDetails paymentSumDetails) {
    this.paymentSumDetails = paymentSumDetails;
    initData();
  }


  @FXML
  private void onDigitPress(ActionEvent event) {
    if (cardButton.isSelected()) {
      return;
    }
    String digit = ((Button) event.getSource()).getText();
    String previousValue = activePaymentField.getText();
    activePaymentField.setText(previousValue + digit);
    setFocusOnActivePaymentField();
  }

  private void setFocusOnActivePaymentField() {
    activePaymentField.requestFocus();
    activePaymentField.positionCaret(activePaymentField.getText().length());
  }

  private boolean checkPaymentSumEnough() {
    if (cashButton.isSelected()) {
      fillPaymentSumDetails(PaymentType.CASH, paymentSumDetails.getTotalToPay(), 0);
      return paymentSumDetails.getTotalToPay() <= paymentSumDetails.getReceivedPayment();
    } else if (cardButton.isSelected()) {
      fillPaymentSumDetails(PaymentType.CARD, 0.0, paymentSumDetails.getTotalToPay());
      return paymentSumDetails.getTotalToPay() <= paymentSumDetails.getReceivedPayment();
    } else if (mixedButton.isSelected()) {
      double cash = UtilNumbers.parseDoubleAmount(mixedViewController.mixedCashField.getText());
      double card = UtilNumbers.parseDoubleAmount(mixedViewController.mixedCardField.getText());
      fillPaymentSumDetails(PaymentType.MIXED, cash, card);
      return (cash + card) == paymentSumDetails.getTotalToPay();
    }
    return false;
  }

  private void fillPaymentSumDetails(PaymentType paymentType, double cash, double card) {
    paymentSumDetails.setPaymentType(paymentType);
    paymentSumDetails.setCashPayment(cash);
    paymentSumDetails.setCardPayment(card);
  }

  @FXML
  private void onBackspace() {
    String fieldValue = activePaymentField.getText();
    if (!fieldValue.isEmpty() && !cardButton.isSelected()) {
      activePaymentField.setText(fieldValue.substring(0, fieldValue.length() - 1));
      setFocusOnActivePaymentField();
    }
  }

  @FXML
  private void onAmountAdd(ActionEvent event) {
    if (cardButton.isSelected()) {
      return;
    }
    Button btn = (Button) event.getSource();
    String text = btn.getText().replace("+", "").trim();
    double buttonSum = UtilNumbers.parseDoubleAmount(text);
    double result = UtilNumbers.parseDoubleAmount(activePaymentField.getText()) + buttonSum;
    activePaymentField.setText(String.valueOf(result));
    setFocusOnActivePaymentField();
  }

  @FXML
  private void onPay() {
    if (!checkPaymentSumEnough()) {
      throw new ValidationException("Недостаточно суммы для оплаты");
    }
    paymentConfirmed = true;
    handleClose();
  }

  @FXML
  private void onCancel() {
    paymentConfirmed = false;
    handleClose();
  }

  private void updateFromInputNew() {
    try {
      double receivedAmount = UtilNumbers.parseDoubleAmount(activePaymentField.getText());
      if (mixedButton.isSelected()) {
        var remainingForCardPayment = paymentSumDetails.getTotalToPay() - receivedAmount;
        if (remainingForCardPayment > 0) {
          mixedViewController.mixedCardField.setText(String.valueOf(remainingForCardPayment));
          paymentSumDetails.setReceivedPayment(paymentSumDetails.getTotalToPay());
          return;
        } else {
          mixedViewController.mixedCardField.setText(String.valueOf(0));
        }
      }
      paymentSumDetails.setReceivedPayment(receivedAmount);
    } catch (NumberFormatException e) {
      paymentSumDetails.setReceivedPayment(0);
    }
    updateLabelValues();
  }

  private void updateLabelValues() {
    totalLabel.setText(String.format("%,.2f тг", paymentSumDetails.getTotalToPay()));
    receivedPaymentLabel.setText(String.format("%,.2f тг", paymentSumDetails.getReceivedPayment()));
    remainingPaymentLabel.setText(
        String.format("%,.2f тг", paymentSumDetails.getRemainingPayment()));
    changeMoneyLabel.setText(String.format("%,.2f тг", paymentSumDetails.getChangeMoney()));
  }

  @Override
  public void handleClose() {
    Stage stage = (Stage) paymentWindow.getScene().getWindow();
    stage.close();
  }
}