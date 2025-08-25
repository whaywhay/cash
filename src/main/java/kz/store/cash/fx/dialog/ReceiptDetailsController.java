package kz.store.cash.fx.dialog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import kz.store.cash.model.enums.ReceiptStatus;
import kz.store.cash.service.AppSettingService;
import kz.store.cash.service.SalesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReceiptDetailsController implements CancellableDialog {

  @FXML
  public Label receiptStatusLabel;
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

  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm:ss");
  private final SalesService salesService;
  private final AppSettingService appSettingService;
  private final ProductProperties productProperties;
  private final ReceiptPrintService receiptPrintService;
  private final MainViewController mainViewController;

  private String organizationName;
  private String cashierName;

  private void getAppSetting() {
    var appSetting = appSettingService.getSingleton().orElse(null);
    if (appSetting == null) {
      organizationName = "";
      cashierName = "";
    } else {
      organizationName = appSetting.getOrgName();
      cashierName = appSetting.getSaleStore();
    }
  }

  public void sendReceipt(PaymentReceipt receipt) {
    getAppSetting();
    var sales = salesService.getSalesByPaymentReceipt(receipt);
    LocalDateTime returnPeriodDate = LocalDateTime.now()
        .minusDays(productProperties.productReturnPeriod());
    cashierLabel.setText("КАССИР: " + organizationName);
    cashboxLabel.setText(cashierName);
    receiptIdLabel.setText("ЧЕК #" + receipt.getId());
    dateLabel.setText(formatter.format(receipt.getCreated()));
    receiptStatusLabel.setText(receipt.getReceiptStatus().getDisplayName());

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
    if (receipt.getReceiptStatus().equals(ReceiptStatus.RETURN)
        || receipt.getReceiptStatus().equals(ReceiptStatus.RETURN_NO_RECEIPT)
        || returnPeriodDate.isAfter(receipt.getCreated())) {
      returnBtn.setDisable(true);
    }
  }

  @Override
  public void handleClose() {
    ((Stage) closeBtn.getScene().getWindow()).close();
  }
}