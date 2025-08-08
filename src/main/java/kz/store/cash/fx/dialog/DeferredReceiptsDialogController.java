package kz.store.cash.fx.dialog;

import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;

import javafx.stage.Stage;
import kz.store.cash.fx.dialog.lib.CancellableDialog;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.model.entity.Sales;
import kz.store.cash.service.SalesService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
@Getter
public class DeferredReceiptsDialogController implements CancellableDialog {

  @FXML
  public FlowPane buttonPane;
  @FXML
  public Button plusButton;


  @FXML
  public Button minusButton;

  private List<Sales> salesList;
  private PaymentReceipt paymentReceipt;
  private boolean openDeferredPaymentReceipts = false;
  private boolean dialogClosed = false;
  private final SalesService salesService;


  public void init(PaymentReceipt currentPaymentReceiptId, List<ProductItem> currentCart,
      List<PaymentReceipt> deferredReceipts) {
    paymentReceipt = null;
    salesList = null;
    plusButton.setOnAction(e -> {
      openDeferredPaymentReceipts = false;
      salesService.mergeDeferredPaymentReceipts(currentPaymentReceiptId, currentCart);
      log.info("Добавление нового отложенного чека");
      handleClose();
    });

    for (PaymentReceipt deferredPaymentReceipt : deferredReceipts) {
      Button receiptButton = createDeferredReceiptButton(deferredPaymentReceipt);
      buttonPane.getChildren().add(receiptButton);
    }
  }

  private Button createDeferredReceiptButton(PaymentReceipt deferredPaymentReceipt) {
    String title = "Чек : " + deferredPaymentReceipt.getId();
    Button receiptButton = new Button(title);
    receiptButton.setPrefSize(120, 60);
    receiptButton.setWrapText(true);
    receiptButton.setStyle(
        "-fx-font-size: 24px; -fx-background-color: white; -fx-border-color: teal;");
    receiptButton.setOnAction(e -> {
      // логика загрузки отложенного чека
      paymentReceipt = deferredPaymentReceipt;
      openDeferredPaymentReceipts = true;
      log.info("Открытие чека: {}", deferredPaymentReceipt.getId());
      handleClose();
    });
    return receiptButton;
  }

  @Override
  public void handleClose() {
    Stage stage = (Stage) buttonPane.getScene().getWindow();
    stage.close();
  }

  public void onClose() {
    dialogClosed = true;
    handleClose();
  }
}
