package kz.store.cash.fx.dialog;

import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;

import javafx.stage.Stage;
import kz.store.cash.fx.dialog.lib.CancellableDialog;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.model.entity.PaymentReceipt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class DeferredReceiptsDialogController implements CancellableDialog {

  @FXML
  public FlowPane buttonPane;
  @FXML
  public Button plusButton;

  public void init(Long currentPaymentReceiptId, List<ProductItem> currentCart,
      List<PaymentReceipt> deferredReceipts) {
    plusButton.setOnAction(e -> log.info("Добавление нового отложенного чека"));

    for (PaymentReceipt receipt : deferredReceipts) {
      String title = "Чек : " + receipt.getId();
      Button receiptButton = new Button(title);
      receiptButton.setPrefSize(120, 60);
      receiptButton.setWrapText(true);
      receiptButton.setStyle(
          "-fx-font-size: 24px; -fx-background-color: white; -fx-border-color: teal;");
      receiptButton.setOnAction(e -> {
        // логика загрузки отложенного чека
        log.info("Открытие чека: {}", receipt.getId());
      });

      buttonPane.getChildren().add(receiptButton);
    }
  }

  @Override
  public void handleClose() {
    Stage stage = (Stage) buttonPane.getScene().getWindow();
    stage.close();
  }

  public void onClose() {
    handleClose();
  }
}
