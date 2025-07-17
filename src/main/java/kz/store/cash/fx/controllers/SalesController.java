package kz.store.cash.fx.controllers;


import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.util.Duration;
import kz.store.cash.fx.controllers.payments.PaymentDialogController;
import kz.store.cash.fx.model.PaymentSumDetails;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.fx.scanner.BarcodeScannerListener;
import kz.store.cash.model.enums.PriceMode;
import kz.store.cash.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class SalesController {

  @FXML
  public TextField barCode;
  @FXML
  public Label priceHeaderLabel;
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
  public Label changeMoneyLabel;
  @FXML
  public Label receivedPaymentLabel;
  @FXML
  private Label matchStatusIcon;

  private final ObservableList<ProductItem> cart = FXCollections.observableArrayList();
  private final ProductService productService;
  private BarcodeScannerListener scannerListener;
  private final ContextMenu suggestionsPopup = new ContextMenu();
  private final PauseTransition debounceTimer = new PauseTransition(Duration.millis(300));
  private PriceMode currentPriceMode = PriceMode.ORIGINAL;
  private PaymentSumDetails paymentSumDetails;


  @FXML
  public void initialize() {
    initTableView();

    Platform.runLater(() -> {
      Scene scene = salesTable.getScene();
      if (scene != null) {
        scannerListener = new BarcodeScannerListener(this::handleBarcodeScanned);
        scannerListener.attachTo(scene);
      }
    });

    // Ручной ввод через TextField
    barCode.textProperty().addListener((obs, oldText, newText) -> {
      if (newText.length() >= 5) {
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
    double total = cart.stream()
        .mapToDouble(ProductItem::getTotal)
        .sum();
    paymentSumDetails = new PaymentSumDetails(total, 0);
    updatePaymentDetailSumLabel(paymentSumDetails);
  }

  private void updatePaymentDetailSumLabel(PaymentSumDetails paymentSum) {
    totalLabel.setText(String.format("%.2f тг", paymentSum.getTotalToPay()));
    receivedPaymentLabel.setText(String.format("%.2f тг", paymentSum.getReceivedPayment()));
    changeMoneyLabel.setText(String.format("%.2f тг", paymentSum.getChangeMoney()));
  }

  private void updateHeaderCheckboxState() {
    if (cart.isEmpty()) {
      headerCheckBox.setSelected(false);
      headerCheckBox.setIndeterminate(false);
      return;
    }
    long selectedCount = cart.stream().filter(ProductItem::isSelected).count();
    if (selectedCount == cart.size()) {
      headerCheckBox.setSelected(true);
      headerCheckBox.setIndeterminate(false);
    } else if (selectedCount == 0) {
      headerCheckBox.setSelected(false);
      headerCheckBox.setIndeterminate(false);
    } else {
      headerCheckBox.setIndeterminate(true);
    }
  }

  public void addOrUpdateProduct(ProductItem productItem) {
    ProductItem existing = cart.stream()
        .filter(p -> p.getBarcode().equals(productItem.getBarcode()))
        .findFirst()
        .orElse(null);
    if (existing != null) {
      existing.increaseQuantity();
      salesTable.getSelectionModel().select(existing);
    } else {
      cart.add(productItem);
      salesTable.getSelectionModel().select(productItem);
    }
    updateTotal();
  }

  @FXML
  private void onIncreaseQuantity() {
    ProductItem selected = salesTable.getSelectionModel().getSelectedItem();
    if (selected != null) {
      selected.increaseQuantity();
      updateTotal();
    }
  }

  @FXML
  private void onDecreaseQuantity() {
    ProductItem selected = salesTable.getSelectionModel().getSelectedItem();
    if (selected != null) {
      selected.decreaseQuantity();
      updateTotal();
    }
  }

  @FXML
  private void onDeleteSelected() {
    cart.removeIf(ProductItem::isSelected);
    ProductItem selected = salesTable.getSelectionModel().getSelectedItem();
    cart.remove(selected);
    updateTotal();
  }

  @FXML
  private void onPay() {
    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Чек оплачен!");
    alert.showAndWait();
    cart.clear();
    updateTotal();
  }

  private void showError(String msg) {
    Alert alert = new Alert(AlertType.ERROR, msg);
    alert.setTitle("Ошибка поиска товара");
    alert.setHeaderText(null);
    alert.showAndWait();
  }

  private void handleBarcodeScanned(String barcode) {
    ProductItem product = productService.findByBarcode(barcode);
    if (product != null) {
      showMatchIndicator(true);
      addOrUpdateProduct(product);
    } else {
      showMatchIndicator(false);
      showError("Товар не найден: " + barcode);
    }

//    barCode.setText(barcode);
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
    salesTable.setItems(cart);
    salesTable.setEditable(true);
    checkboxCol.setCellValueFactory(cell -> cell.getValue().selectedProperty());
    checkboxCol.setCellFactory(CheckBoxTableCell.forTableColumn(checkboxCol));
    checkboxCol.setEditable(true);
    indexCol.setCellValueFactory(
        cell -> new ReadOnlyObjectWrapper<>(salesTable.getItems().indexOf(cell.getValue()) + 1));
    nameCol.setCellValueFactory(cell -> cell.getValue().nameProperty());
    priceCol.setCellValueFactory(cell -> cell.getValue().priceProperty());
    qtyCol.setCellValueFactory(cell -> cell.getValue().quantityProperty());
    totalCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getTotal()));
    cart.addListener((ListChangeListener<ProductItem>) change -> {
      updateTotal();
      updateHeaderCheckboxState();
    });
    cart.forEach(item -> item.selectedProperty()
        .addListener((obs, oldVal, newVal) -> updateHeaderCheckboxState()));
    setupPriceHeaderMenu();
  }

  private void showSuggestions(String input) {
    List<ProductItem> matches = productService.searchByPartialBarcode(input);
    if (matches.isEmpty()) {
      suggestionsPopup.hide();
      return;
    }
    List<CustomMenuItem> menuItems = matches.stream().limit(10).map(item -> {
      Label suggestionLabel = new Label(item.getBarcode() + " - " + item.getName());
      suggestionLabel.setStyle("-fx-padding: 5px;");
      CustomMenuItem menuItem = new CustomMenuItem(suggestionLabel, true);
      menuItem.setOnAction(e -> {
        addOrUpdateProduct(item); // сразу добавляем
        barCode.clear();                  // очищаем поле
        suggestionsPopup.hide();         // скрываем меню
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

  public void showPaymentDialog() {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/payment/payment_dialog.fxml"));
    try {
      DialogPane dialogPane = loader.load();
      PaymentDialogController controller = loader.getController();
      controller.setPaymentDetailsSum(paymentSumDetails);

      Dialog<ButtonType> dialog = new Dialog<>();
      dialog.setDialogPane(dialogPane);
      dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
      Optional<ButtonType> result = dialog.showAndWait();
      if (result.isPresent() &&  result.get() == ButtonType.OK) {
        //getResult of paymentSumDetails after that some operation
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


}
