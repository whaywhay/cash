package kz.store.cash.fx.dialog;

import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;

import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import kz.store.cash.fx.dialog.lib.CancellableDialog;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.fx.model.SalesWithProductName;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.model.entity.Sales;
import kz.store.cash.service.PaymentReceiptService;
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
  private FlowPane buttonPane;
  @FXML
  private Button plusButton;
  @FXML
  private ContextMenu previewMenu;
  @FXML
  private CustomMenuItem previewItem;
  @FXML
  private Label previewTitle;
  @FXML
  private Label previewMeta;
  @FXML
  private VBox previewLinesContainer;
  @FXML
  private MenuItem chooseItem;

  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm:ss");
  private List<Sales> salesList;
  private PaymentReceipt paymentReceipt;
  private boolean openDeferredPaymentReceipts = false;
  private boolean dialogClosed = false;

  private final PaymentReceiptService paymentReceiptService;
  private final SalesService salesService;

  // для кнопки "Выбрать"/пункта меню — текущий выбранный (последняя кликнутая кнопка)
  private PaymentReceipt currentSelected;

  public void init(PaymentReceipt currentPaymentReceipt,
      List<ProductItem> currentCart,      List<PaymentReceipt> deferredReceipts) {

    paymentReceipt = null;
    salesList = null;
    currentSelected = null;
    // "+" — мердж текущей корзины в отложенный чек
    plusButton.setOnAction(e -> {
      openDeferredPaymentReceipts = false;
      dialogClosed = false;
      paymentReceiptService.mergeDeferredPaymentReceipts(currentPaymentReceipt, currentCart);
      handleClose();
    });

    // Рисуем кнопки чеков
    buttonPane.getChildren().clear();
    for (PaymentReceipt pr : deferredReceipts) {
      if (currentPaymentReceipt == null || !currentPaymentReceipt.getId().equals(pr.getId())) {
        Button btn = new Button("Чек #" + pr.getId());
        btn.getStyleClass().add("receipt-btn");
        // Клик: ЛКМ-1 — превью; Двойной ЛКМ — открыть
        btn.setOnMouseClicked(me -> {
          currentSelected = pr;

          if (me.getButton() != MouseButton.PRIMARY) {
            return;
          }
          // таймер одного клика
          PauseTransition singleClickTimer = new PauseTransition(Duration.millis(220));
          singleClickTimer.setOnFinished(ev ->
              showPreviewFor(pr, btn, me.getScreenX(), me.getScreenY())
          );

          if (me.getClickCount() == 2) {
            // двойной клик: отменяем превью, прячем меню, открываем чек
            singleClickTimer.stop();
            previewMenu.hide();
            openSelectedReceipt(pr);
          } else {
            // готовим превью; если второй клик придёт за 220ms, он отменит показ
            singleClickTimer.playFromStart();
          }
        });
        buttonPane.getChildren().add(btn);
      }
    }
  }

  private void showPreviewFor(PaymentReceipt pr, Node owner, double screenX, double screenY) {
    previewTitle.setText("Чек #" + pr.getId());
    previewMeta.setText(formatMeta(pr));
    previewLinesContainer.getChildren().clear();
    List<SalesWithProductName> rows = salesService.getSalesByPaymentReceipt(pr);
    int limit = Math.min(rows.size(), 5);
    for (int i = 0; i < limit; i++) {
      var r = rows.get(i);
      Label line = new Label(r.productName() + " × " + r.quantity() + " = " + r.total());
      line.getStyleClass().add("preview-line");
      previewLinesContainer.getChildren().add(line);
    }
    if (rows.size() > limit) {
      Label more = new Label("…");
      more.getStyleClass().add("preview-line");
      previewLinesContainer.getChildren().add(more);
    }
    // Показать меню у курсора
    previewMenu.show(owner, screenX, screenY);
  }

  private String formatMeta(PaymentReceipt pr) {
    var created = pr.getCreated(); // из BaseEntity
    return created != null ? formatter.format(created) : "";
  }

  private void openSelectedReceipt(PaymentReceipt pr) {
    previewMenu.hide();
    paymentReceipt = pr;
    openDeferredPaymentReceipts = true;
    dialogClosed = false;
    handleClose();
  }

  // Кнопка "ВЫБРАТЬ" и пункт меню "Выбрать"
  @FXML
  public void onSelect() {
    if (currentSelected != null) {
      openSelectedReceipt(currentSelected);
    }
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