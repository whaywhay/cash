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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import kz.store.cash.fx.component.SalesCartService;
import kz.store.cash.fx.component.TableViewProductConfigService;
import kz.store.cash.fx.component.UiNotificationService;
import kz.store.cash.fx.dialog.AdditionalFunctionDialogController;
import kz.store.cash.fx.dialog.CategoryProductDialogController;
import kz.store.cash.fx.dialog.DeferredReceiptsDialogController;
import kz.store.cash.fx.dialog.DiaryCustomersDialogController;
import kz.store.cash.fx.dialog.QuantitySetDialogController;
import kz.store.cash.fx.dialog.QuickProductsDialogController;
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
import kz.store.cash.service.SalesService;
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
  public Label deferredPaymentReceipt;
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
  private PaymentReceipt paymentReceipt;
  private final SalesService salesService;
  private final UiNotificationService uiNotificationService;

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

    qtyCol.setCellFactory(col -> {
      TableCell<ProductItem, Number> cell = new TableCell<>() {
        @Override
        protected void updateItem(Number value, boolean empty) {
          super.updateItem(value, empty);
          setText(empty || value == null ? null : String.valueOf(value.intValue()));
        }
      };

      cell.setOnMouseClicked(e -> {
        if (e.getButton() == MouseButton.PRIMARY
            && e.getClickCount() == 1
            && !cell.isEmpty()) {
          salesTable.getSelectionModel().select(cell.getIndex());
          onQuantityDialog();
          e.consume();
        }
      });

      return cell;
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
  private void onDeleteCheckedItems() {
    cart.removeIf(ProductItem::isSelected);
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
        System.out.println("after onPaymentDialog - paymentSumDetails" + paymentSumDetails);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void processPaymentSuccess(PaymentSumDetails paymentSumDetails) {
    try {
      if (paymentReceipt == null) {
        paymentReceiptService.processPaymentSave(paymentSumDetails, cart);
      } else {
        paymentReceiptService.processPaymentSaveWithDeferredPayment(paymentReceipt,
            paymentSumDetails, cart);
        refreshDeferredPaymentReceiptsUI();
      }
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
      uiNotificationService.showError(e.getMessage());
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
      uiNotificationService.showError(e.getMessage());
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
      uiNotificationService.showError(e.getMessage());
    }
  }

  @FXML
  public void onDeferredReceiptsDialog() {
    try {
      var loader = dialogBase.loadFXML("/fxml/sales/deferred_receipts_dialog.fxml");
      VBox openedRoot = loader.load();
      DeferredReceiptsDialogController controller = loader.getController();
      controller.init(paymentReceipt, cart, getDeferredPaymentReceipts());
      dialogBase.createDialogStage(rootPane, openedRoot, controller);
      if (!controller.isDialogClosed()) {
        if (controller.isOpenDeferredPaymentReceipts()) {
          var saleList = salesService.getSalesByPaymentReceipt(controller.getPaymentReceipt());
          cart.clear();
          cart.addAll(salesCartService.getMapSalesToProductItem(saleList));
          setDeferredPaymentReceiptsToUI(controller.getPaymentReceipt());
        } else {
          cart.clear();
          refreshDeferredPaymentReceiptsUI();
        }
      }
    } catch (IOException e) {
      log.error("IOException in SalesController.onDeferredReceiptsDialog()", e);
      uiNotificationService.showError(e.getMessage());
    }
  }

  private List<PaymentReceipt> getDeferredPaymentReceipts() {
    return paymentReceiptService.getDeferredPaymentReceipts();
  }

  private void refreshDeferredPaymentReceiptsUI() {
    paymentReceipt = null;
    deferredPaymentReceipt.setText("");
  }

  private void setDeferredPaymentReceiptsToUI(PaymentReceipt paymentReceipt) {
    this.paymentReceipt = paymentReceipt;
    deferredPaymentReceipt.setText("Отложенный чек: #" + paymentReceipt.getId());
  }

  public void onCategoryProductDialog() {
    try {
      var loader = dialogBase.loadFXML("/fxml/sales/category_product_browser.fxml");
      VBox openedRoot = loader.load();
      CategoryProductDialogController controller = loader.getController();
      controller.init(currentPriceMode, this::addOrUpdateProduct); // передаём режим цен и коллбек
      dialogBase.createDialogStage(rootPane, openedRoot, controller);
    } catch (IOException e) {
      log.error("IOException in SalesController.onCategoryProductDialog()", e);
      uiNotificationService.showError(e.getMessage());
    }
  }

  public void onAdditionalFunctionDialog() {
    try {
      var loader = dialogBase.loadFXML("/fxml/sales/additional_function_dialog.fxml");
      VBox openedRoot = loader.load();
      AdditionalFunctionDialogController controller = loader.getController();
      dialogBase.createDialogStage(rootPane, openedRoot, controller);
    } catch (IOException e) {
      log.error("IOException in SalesController.onAdditionalFunctionDialog()", e);
      uiNotificationService.showError(e.getMessage());
    }
  }

  public void onQuickProductsDialog() {
    try {
      var loader = dialogBase.loadFXML("/fxml/sales/quick_products_dialog.fxml");
      VBox openedRoot = loader.load();
      QuickProductsDialogController controller = loader.getController();
      controller.init(currentPriceMode, this::addOrUpdateProduct);
      dialogBase.createDialogStage(rootPane, openedRoot, controller);
    } catch (IOException e) {
      log.error("IOException in SalesController.onQuickProductsDialog()", e);
      uiNotificationService.showError(e.getMessage());
    }
  }

  public void onDiaryDebtDialog() {
    try {
      var loader = dialogBase.loadFXML("/fxml/sales/customers_dialog.fxml");
      BorderPane openedRoot = loader.load();
      DiaryCustomersDialogController controller = loader.getController();
      controller.resetAndReload(paymentSumDetails == null ? 0 : paymentSumDetails.getTotalToPay());
      dialogBase.createFullscreenDialogStage(rootPane, openedRoot, controller);
    } catch (IOException e) {
      log.error("IOException in SalesController.onDiaryDebt()", e);
      uiNotificationService.showError(e.getMessage());
    }
  }
}
