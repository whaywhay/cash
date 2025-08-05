package kz.store.cash.fx.dialog;

import java.math.BigDecimal;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import kz.store.cash.config.ProductProperties;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.fx.component.ReceiptPrintService;
import kz.store.cash.fx.controllers.MainViewController;
import kz.store.cash.fx.dialog.lib.CancellableDialog;
import kz.store.cash.fx.model.SalesWithProductName;
import kz.store.cash.model.enums.PaymentType;
import kz.store.cash.repository.SalesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReceiptDetailsController implements CancellableDialog {

  @FXML
  private Label cashierLabel;
  @FXML
  private Label cashboxLabel;
  @FXML
  private Label receiptIdLabel;
  @FXML
  private Label dateLabel;
  @FXML
  private VBox itemsContainer;
  @FXML
  private Label totalLabel;
  @FXML
  private Label paymentTypeLabel;
  @FXML
  private Label changeLabel;
  @FXML
  private Button closeBtn;
  @FXML
  private Button returnBtn;
  @FXML
  private Button printBtn;

  private final SalesRepository salesRepository;
  private final ProductProperties productProperties;
  private final ReceiptPrintService receiptPrintService;
  private final MainViewController mainViewController;

  public void sendReceipt(PaymentReceipt receipt) {
    var sales = salesRepository.findSalesWithProductNames(receipt.getId());

    cashierLabel.setText("КАССИР: " + productProperties.organizationName());
    cashboxLabel.setText(productProperties.cashierName());
    receiptIdLabel.setText("ЧЕК #" + receipt.getId());
    dateLabel.setText(receipt.getCreated().toString());

    itemsContainer.getChildren().clear();
    for (SalesWithProductName sale : sales) {
      Label itemLabel = new Label(
          String.format("%s\n%d x %.2f = %.2f",
              sale.productName(), sale.quantity(), sale.soldPrice(), sale.total())
      );
      itemLabel.setStyle("-fx-font-size: 14px;");
      itemsContainer.getChildren().add(itemLabel);
    }

    totalLabel.setText("ИТОГО: " + receipt.getTotal() + " тг");
    if (receipt.getPaymentType() == PaymentType.MIXED) {
      String mixedDetails =
          "Смешанная оплата: " + receipt.getReceivedPayment() + "\n" + "Наличные: "
              + (receipt.getCashPayment() != null ? receipt.getCashPayment() : BigDecimal.ZERO)
              + " тг\n"
              + "Карта: "
              + (receipt.getCardPayment() != null ? receipt.getCardPayment() : BigDecimal.ZERO)
              + " тг";
      paymentTypeLabel.setText(mixedDetails);
    } else {
      paymentTypeLabel.setText(
          receipt.getPaymentType().getDisplayName() + ": " + receipt.getReceivedPayment());
    }
    changeLabel.setText("Сдача: " + receipt.getChangeMoney());

    closeBtn.setOnAction(e -> handleClose());
    returnBtn.setOnAction(e -> {
      handleClose(); // Закрыть диалог
      mainViewController.openReturnTab(receipt, sales);
    });
    printBtn.setOnAction(e -> receiptPrintService.printReceiptRawWithLine(receipt));
  }

  @Override
  public void handleClose() {
    ((Stage) closeBtn.getScene().getWindow()).close();
  }
}