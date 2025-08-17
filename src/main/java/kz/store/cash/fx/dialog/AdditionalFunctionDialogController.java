package kz.store.cash.fx.dialog;


import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import kz.store.cash.fx.component.FxAsyncRunner;
import kz.store.cash.fx.component.ReceiptPrintService;
import kz.store.cash.fx.dialog.lib.CancellableDialog;
import kz.store.cash.service.CategoryAndProduct1C;
import kz.store.cash.service.PaymentReceiptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class AdditionalFunctionDialogController implements CancellableDialog {

  @FXML
  public FlowPane additionalFunctionPane;
  @FXML
  public Button exitApplicationButton;

  private final PaymentReceiptService paymentReceiptService;
  private final ReceiptPrintService receiptPrintService;
  private final CategoryAndProduct1C categoryAndProduct1C;
  private final FxAsyncRunner fxAsyncRunner;

  @Override
  public void handleClose() {
    Stage stage = (Stage) additionalFunctionPane.getScene().getWindow();
    stage.close();
  }

  public void onClose() {
    handleClose();
  }

  public void onPrintLastReceipt() {
    var receipt = paymentReceiptService.getNewestSalePaymentReceipt();
    receiptPrintService.printReceiptRawWithLine(receipt);
    handleClose();
  }

  public void onSyncWith1C() {
    fxAsyncRunner.runWithLoader(
        additionalFunctionPane, "Синхронизация с 1С…", categoryAndProduct1C::syncAll1C);
  }
}
