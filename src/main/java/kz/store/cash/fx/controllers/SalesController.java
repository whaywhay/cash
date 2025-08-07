package kz.store.cash.fx.controllers;


import java.io.IOException;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import kz.store.cash.fx.component.SalesCartService;
import kz.store.cash.fx.component.TableViewProductConfigService;
import kz.store.cash.fx.dialog.DeferredReceiptsDialogController;
import kz.store.cash.fx.dialog.QuantitySetDialogController;
import kz.store.cash.fx.dialog.UniversalProductDialogController;
import kz.store.cash.fx.dialog.lib.DialogBase;
import kz.store.cash.fx.dialog.payments.PaymentDialogController;
import kz.store.cash.fx.dialog.EditProductDialogController;
import kz.store.cash.fx.model.PaymentSumDetails;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.fx.component.BarcodeScannerListener;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.model.enums.PriceMode;
import kz.store.cash.service.PaymentReceiptService;
import kz.store.cash.service.ProductService;
import kz.store.cash.util.TableUtils;
import kz.store.cash.util.UtilAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesController {

  @FXML
  public TextField barCode;
  @FXML
  public Label priceHeaderLabel;
  @FXML
  public BorderPane rootPane;
  @FXML
  private TableView<ProductItem> salesTable;
  @FXML
  private TableColumn<ProductItem, Boolean> checkboxCol;
  @FXML
  private TableColumn<ProductItem, Number> indexCol;
  @FXML
  private TableColumn<ProductItem, String> nameCol;
  @FXML
  private TableColumn<ProductItem, Number> priceCol;
  @FXML
  private TableColumn<ProductItem, Number> qtyCol;
  @FXML
  private TableColumn<ProductItem, Number> totalCol;
  @FXML
  private CheckBox headerCheckBox;
  @FXML
  private Label totalLabel;
  @FXML
  private Label changeMoneyLabel;
  @FXML
  private Label receivedPaymentLabel;
  @FXML
  private Label matchStatusIcon;

  private final ObservableList<ProductItem> cart = FXCollections.observableArrayList();
  private final ProductService productService;
  private final DialogBase dialogBase;
  private final UtilAlert utilAlert;
  private final PaymentReceiptService paymentReceiptService;
  private final SalesCartService salesCartService;
  private final TableViewProductConfigService tableViewProductConfigService;
  private BarcodeScannerListener scannerListener;
  private final ContextMenu suggestionsPopup = new ContextMenu();
  private final PauseTransition debounceTimer = new PauseTransition(Duration.millis(300));
  private PriceMode currentPriceMode = PriceMode.ORIGINAL;
  private PaymentSumDetails paymentSumDetails;

  @FXML
  public void initialize() {
    initTableView();
    TableUtils.bindColumnWidths(
        salesTable,
        new double[]{0.04, 0.06, 0.42, 0.15, 0.15, 0.18},
        // размер для колонок 1 = 100% процент размеру
        checkboxCol, indexCol, nameCol, priceCol, qtyCol, totalCol);
    Platform.runLater(() -> {
      salesTable.requestFocus();
      Scene scene = salesTable.getScene();
      if (scene != null) {
        scannerListener = new BarcodeScannerListener(this::handleBarcodeScanned);
        scannerListener.attachTo(scene);
      }
    });

    // Ручной ввод через TextField
    barCode.textProperty().addListener((obs, oldText, newText) -> {
      if (newText.length() >= 3) {
        debounceTimer.setOnFinished(e -> showSuggestions(newText));
        debounceTimer.playFromStart();
      } else {
        suggestionsPopup.hide();
      }
    });

    // Закрытие подсказок при фокусе вне поля
    barCode.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
      if (!isNowFocused) {
        suggestionsPopup.hide();
      }
    });
  }

  private void updateTotal() {
    double total = salesCartService.calculateTotal(cart);
    paymentSumDetails = new PaymentSumDetails(total, 0);
    updatePaymentDetailSumLabel(paymentSumDetails);
    barCode.clear();
  }

  private void updatePaymentDetailSumLabel(PaymentSumDetails paymentSum) {
    totalLabel.setText(String.format("%.2f тг", paymentSum.getTotalToPay()));
    receivedPaymentLabel.setText(String.format("%.2f тг", paymentSum.getReceivedPayment()));
    changeMoneyLabel.setText(String.format("%.2f тг", paymentSum.getChangeMoney()));
  }

  public void addOrUpdateProduct(ProductItem productItem) {
    salesCartService.increaseOrAddNewToCart(cart, productItem, salesTable);
    updateTotal();
  }

  private void addUniversalProduct(double price) {
    var productItem = productService.findUniversalProduct(price);
    if (productItem != null) {
      cart.add(productItem);
      salesTable.getSelectionModel().select(productItem);
    }
  }

  @FXML
  private void onIncreaseQuantity() {
    ProductItem selected = salesTable.getSelectionModel().getSelectedItem();
    if (selected != null) {
      selected.increaseQuantity();
      updateTotal();
      salesTable.refresh();
    }
  }

  @FXML
  private void onDecreaseQuantity() {
    ProductItem selected = salesTable.getSelectionModel().getSelectedItem();
    if (selected != null) {
      selected.decreaseQuantity();
      updateTotal();
      salesTable.refresh();
    }
  }

  @FXML
  private void onDeleteSelectedOrChecked() {
    cart.removeIf(ProductItem::isSelected);
    ProductItem selected = salesTable.getSelectionModel().getSelectedItem();
    cart.remove(selected);
    updateTotal();
  }

  private void handleBarcodeScanned(String barcode) {
    ProductItem product = productService.findByBarcode(barcode);
    if (product != null) {
      showMatchIndicator(true);
      switchPriceModeForOneItem(product);
      addOrUpdateProduct(product);
    } else {
      showMatchIndicator(false);
      utilAlert.showError("Ошибка поиска товара", "Товар не найден: " + barcode);
    }
  }

  private void showMatchIndicator(boolean found) {
    FontIcon statusIcon = new FontIcon();
    if (found) {
      statusIcon.setIconLiteral("fas-check-circle");
      statusIcon.getStyleClass().add("icon-success");
      matchStatusIcon.setGraphic(statusIcon);
    } else {
      statusIcon.setIconLiteral("fas-times-circle");
      statusIcon.getStyleClass().add("icon-error");
      matchStatusIcon.setGraphic(statusIcon);
    }
  }

  public void selectHeaderCheckBox() {
    boolean selected = headerCheckBox.isSelected();
    cart.forEach(item -> item.setSelected(selected));
  }

  public void initTableView() {
    //          updateHeaderCheckboxState();
    tableViewProductConfigService.configure(salesTable, checkboxCol, indexCol, nameCol, priceCol,
        qtyCol, totalCol, cart, headerCheckBox,
        this::updateTotal
    );
    setupPriceHeaderMenu();
  }

  private void showSuggestions(String input) {
    List<ProductItem> matches = productService.searchByPartialBarcode(input);
    if (matches.isEmpty()) {
      suggestionsPopup.hide();
      return;
    }
    List<CustomMenuItem> menuItems = matches.stream().limit(10).map(item -> {
      Label suggestionLabel = new Label(item.getBarcode() + " - " + item.getProductName());
      suggestionLabel.setStyle("-fx-padding: 5px;");
      CustomMenuItem menuItem = new CustomMenuItem(suggestionLabel, true);
      menuItem.setOnAction(e -> {
        switchPriceModeForOneItem(item);
        addOrUpdateProduct(item);
        barCode.clear();
        suggestionsPopup.hide();
      });
      return menuItem;
    }).toList();
    suggestionsPopup.getItems().setAll(menuItems);
    if (!suggestionsPopup.isShowing()) {
      suggestionsPopup.show(barCode, Side.BOTTOM, 0, 0);
    }
  }

  private void setupPriceHeaderMenu() {
    ContextMenu menu = new ContextMenu();
    for (PriceMode mode : PriceMode.values()) {
      MenuItem item = new MenuItem(mode.getDisplayName());
      item.setOnAction(e -> switchPriceMode(mode));
      menu.getItems().add(item);
    }
    priceHeaderLabel.setOnMouseClicked(e -> {
      if (!menu.isShowing()) {
        menu.show(priceHeaderLabel, Side.BOTTOM, 0, 0);
      } else {
        menu.hide();
      }
    });
    priceHeaderLabel.setText(currentPriceMode.getDisplayName());
  }

  private void switchPriceMode(PriceMode mode) {
    this.currentPriceMode = mode;
    for (ProductItem item : cart) {
      if (mode == PriceMode.ORIGINAL) {
        item.setToOriginalPrice();
      } else {
        item.setToWholesalePrice();
      }
    }
    priceHeaderLabel.setText(currentPriceMode.getDisplayName());
    updateTotal();
    salesTable.refresh();
  }

  private void switchPriceModeForOneItem(ProductItem productItem) {
    if (productItem != null) {
      if (currentPriceMode == PriceMode.WHOLESALE) {
        productItem.setToWholesalePrice();
      } else {
        productItem.setToOriginalPrice();
      }
    }
  }

  public void onPaymentDialog() {
    if (salesTable.getItems().isEmpty()) {
      return;
    }
    try {
      var loader = dialogBase.loadFXML("/fxml/sales/payment/payment_dialog.fxml");
      DialogPane openedRoot = loader.load();
      PaymentDialogController controller = loader.getController();
      controller.setPaymentDetailsSum(paymentSumDetails);
      dialogBase.createDialogStage(rootPane, openedRoot, controller);
      if (controller.isPaymentConfirmed()) {
        paymentSumDetails = controller.getPaymentSumDetails();
        processPaymentSuccess(paymentSumDetails); // свой метод обработки результата
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void processPaymentSuccess(PaymentSumDetails paymentSumDetails) {
    try {
      paymentReceiptService.processPaymentSave(paymentSumDetails, cart);
      cart.clear();
      updatePaymentDetailSumLabel(paymentSumDetails);
      salesTable.requestFocus();
    } catch (Exception e) {
      log.error("Ошибка сохранения продажи", e);
      utilAlert.showError("Ошибка", "Не удалось сохранить продажу. Попробуйте снова.");
    }
  }

  @FXML
  private void onEditProduct() {
    ProductItem selected = salesTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      return;
    }
    try {
      var loader = dialogBase.loadFXML("/fxml/sales/edit_product_dialog.fxml");
      VBox openedRoot = loader.load();
      EditProductDialogController controller = loader.getController();
      controller.setProductItem(selected);
      dialogBase.createDialogStage(rootPane, openedRoot, controller);
      if (controller.getUpdatedProduct() != null) {
        salesTable.getSelectionModel().select(selected);
        updateTotal();
        salesTable.refresh();
      }
    } catch (IOException e) {
      log.info("IOException in SalesController.onEditProduct()", e);
      throw new RuntimeException(e);
    }
  }

  @FXML
  private void onUniversalProductDialog() {
    try {
      var loader = dialogBase.loadFXML("/fxml/sales/universal_product.fxml");
      VBox openedRoot = loader.load();
      UniversalProductDialogController controller = loader.getController();
      controller.initData();
      dialogBase.createDialogStage(rootPane, openedRoot, controller);
      double price = controller.getPriceUniversalProduct();
      if (price > 0) {
        addUniversalProduct(price);
      }
    } catch (IOException e) {
      log.info("IOException in SalesController.onUniversalProductDialog()", e);
      throw new RuntimeException(e);
    }
  }

  @FXML
  public void onQuantityDialog() {
    ProductItem selected = salesTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      return;
    }
    try {
      var loader = dialogBase.loadFXML("/fxml/sales/quantity_dialog.fxml");
      VBox openedRoot = loader.load();
      QuantitySetDialogController controller = loader.getController();
      controller.setProductItem(selected);
      dialogBase.createDialogStage(rootPane, openedRoot, controller);
      if (controller.getUpdatedProduct() != null) {
        updateTotal();
        salesTable.refresh();
      }
    } catch (IOException e) {
      log.error("IOException in SalesController.onQuantityDialog()", e);
      throw new RuntimeException(e);
    }
  }

  @FXML
  public void onDeferredReceiptsDialog() {
    try {
      var loader = dialogBase.loadFXML("/fxml/deferred_receipts_dialog.fxml");
      VBox openedRoot = loader.load();
      DeferredReceiptsDialogController controller = loader.getController();
      controller.init(null, cart, getDeferredPaymentReceipts());
      dialogBase.createDialogStage(rootPane, openedRoot, controller);
      //To-Do something
    } catch (IOException e) {
      log.error("IOException in SalesController.onDeferredReceiptsDialog()", e);
      throw new RuntimeException(e);
    }
  }

  private List<PaymentReceipt> getDeferredPaymentReceipts() {
    return paymentReceiptService.getDeferredPaymentReceipts();
  }
}
