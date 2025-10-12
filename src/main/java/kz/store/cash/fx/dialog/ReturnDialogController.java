package kz.store.cash.fx.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import kz.store.cash.fx.dialog.lib.CancellableDialog;
import kz.store.cash.fx.model.PaymentSumDetails;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.model.enums.PaymentType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReturnDialogController implements CancellableDialog {

  @FXML
  public Button closeBtn;
  @FXML
  public Label totalReturnLabel;
  @FXML
  public Label cashReturnLabel;
  @FXML
  public Label cashLabel;
  @FXML
  public Label cardReturnLabel;
  @FXML
  public Label cardReturnText;
  @FXML
  public Label cardText;
  @FXML
  public Label cardLabel;
  @FXML
  public Label cashText;
  @FXML
  public Label cashReturnText;
  @FXML
  public BorderPane rootPane;

  @Getter
  private PaymentSumDetails paymentSumDetails;
  @Getter
  private boolean returnFlag;

  @Override
  public void handleClose() {
    ((Stage) closeBtn.getScene().getWindow()).close();
  }

  public void onReturn() {
    returnFlag = true;
    handleClose();
  }

  public void onCancel() {
    handleClose();
  }

  public void initData(PaymentReceipt returnPayment, double totalToReturn) {
    returnFlag = false;
    paymentSumDetails = null;
    totalReturnLabel.setText(String.format("%.2f тг", totalToReturn));
    setVisibility(false, cashReturnText, cashReturnLabel, cashText, cashLabel);
    setVisibility(false, cardReturnText, cardReturnLabel, cardText, cardLabel);
    if (returnPayment == null) {
      return;
    }
    double cash = returnPayment.getCashPayment().doubleValue();
    double card = returnPayment.getCardPayment().doubleValue();
    switch (returnPayment.getPaymentType()) {
      case CASH -> {
        setVisibility(true, cashReturnText, cashReturnLabel, cashText, cashLabel);
        cashReturnLabel.setText(String.format("%.2f тг", totalToReturn));
        cashLabel.setText(String.format("%.2f тг", cash));
        setPaymentSumDetails(totalToReturn, totalToReturn, card, PaymentType.CASH);
      }
      case CARD -> {
        setVisibility(true, cardReturnText, cardReturnLabel, cardText, cardLabel);
        cardReturnLabel.setText(String.format("%.2f тг", card));
        cardLabel.setText(String.format("%.2f тг", card));
        setPaymentSumDetails(totalToReturn, cash, card, PaymentType.CARD);
      }
      case MIXED -> {
        setVisibility(true, cashReturnText, cashReturnLabel, cashText, cashLabel);
        setVisibility(true, cardReturnText, cardReturnLabel, cardText, cardLabel);
        cashReturnLabel.setText(String.format("%.2f тг", cash));
        cashLabel.setText(String.format("%.2f тг", cash));
        cardReturnLabel.setText(String.format("%.2f тг", card));
        cardLabel.setText(String.format("%.2f тг", card));
        setPaymentSumDetails(totalToReturn, cash, card, PaymentType.MIXED);
      }
      case DEBT -> setPaymentSumDetails(totalToReturn, 0, 0, PaymentType.DEBT);
    }
  }

  private void setVisibility(boolean visible, Label... labels) {
    for (Label label : labels) {
      label.setVisible(visible);
      label.setManaged(visible);
    }
  }

  private void setPaymentSumDetails(double total, double cash, double card,
      PaymentType paymentType) {
    double totalToReturn = -1 * total;
    paymentSumDetails = new PaymentSumDetails(totalToReturn);
    paymentSumDetails.setPaymentType(paymentType);
    paymentSumDetails.setCashPayment((-1 * cash));
    paymentSumDetails.setCardPayment((-1 * card));
  }
}
