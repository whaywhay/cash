package kz.store.cash.fx.controllers;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import kz.store.cash.fx.dialog.ReturnDialogController;
import kz.store.cash.fx.model.PaymentSumDetails;
import kz.store.cash.handler.ValidationException;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.fx.component.SalesCartService;
import kz.store.cash.fx.component.TableViewProductConfigService;
import kz.store.cash.fx.controllers.lib.TabController;
import kz.store.cash.fx.dialog.QuantitySetDialogController;
import kz.store.cash.fx.dialog.lib.DialogBase;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.fx.model.SalesWithProductName;
import kz.store.cash.mapper.ProductMapper;
import kz.store.cash.model.enums.PaymentType;
import kz.store.cash.service.PaymentReceiptService;
import kz.store.cash.util.TableUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionReturnController implements TabController {

  @FXML
  public Label receiptPaymentIdLabel;
  @FXML
  public Button quantityButton;
  @FXML
  public Button deleteButton;
  @FXML
  public Button searchButton;
  @FXML
  public Label receiptPaymentDateLabel;
  @FXML
  private BorderPane returnPane;
  @FXML
  private RadioButton withPaymentReceiptRadio;
  @FXML
  private RadioButton withoutPaymentReceiptRadio;
  @FXML
  private TextField searchFieldById;
  @FXML
  private TextField searchFieldByProduct;
  @FXML
  private TableView<ProductItem> salesReturnTable;
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
  private Label returnSumLabel;

  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm:ss");
  private boolean openedFromReceipt = false;
  private final ObservableList<ProductItem> cart = FXCollections.observableArrayList();
  private List<SalesWithProductName> returnSales;
  private PaymentReceipt returnPayment;
  private final SalesCartService salesCartService;
  private final TableViewProductConfigService tableViewProductConfigService;
  private final ProductMapper productMapper;
  private final DialogBase dialogBase;
  private final PaymentReceiptService paymentReceiptService;

  @FXML
  public void initialize() {
    initTableView();
    // Привязываем ширину колонок через TableUtils
    TableUtils.bindColumnWidths(
        salesReturnTable,
        new double[]{0.04, 0.06, 0.42, 0.15, 0.15, 0.18},
        // размер для колонок 1 = 100% процент размеру
        checkboxCol, indexCol, nameCol, priceCol, qtyCol, totalCol);
    // Создаем группу и добавляем радиокнопки
    ToggleGroup toggleGroup = new ToggleGroup();
    withPaymentReceiptRadio.setToggleGroup(toggleGroup);
    withoutPaymentReceiptRadio.setToggleGroup(toggleGroup);

    // Состояние по умолчанию
    withPaymentReceiptRadio.setSelected(true);
    searchFieldById.setVisible(true);
    withoutPaymentReceiptRadio.setDisable(false);

    // Слушатель для изменения видимости поля поиска
    toggleGroup.selectedToggleProperty().addListener((obs, old, selected) -> {
      boolean isWithReceipt = selected == withPaymentReceiptRadio;
      configureRadioMenu(isWithReceipt);
    });
  }

  public void initTableView() {
    tableViewProductConfigService.configure(salesReturnTable, checkboxCol, indexCol, nameCol,
        priceCol, qtyCol, totalCol, cart, headerCheckBox,
        this::updateReturnTotal
    );
  }

  private double updateReturnTotal() {
    double total = salesCartService.calculateTotal(cart);
    returnSumLabel.setText(String.format("%.2f тг", total));
    return total;
  }

  @FXML
  public void selectHeaderCheckBox() {
    boolean selected = headerCheckBox.isSelected();
    cart.forEach(item -> item.setSelected(selected));
  }

  @FXML
  public void onDeleteSelectedOrChecked() {
    cart.removeIf(ProductItem::isSelected);
    ProductItem selected = salesReturnTable.getSelectionModel().getSelectedItem();
    cart.remove(selected);
    updateReturnTotal();
  }

  @FXML
  public void onQuantityDialog() {
    ProductItem selected = salesReturnTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
      return;
    }
    try {
      var loader = dialogBase.loadFXML("/fxml/sales/quantity_dialog.fxml");
      VBox openedRoot = loader.load();
      QuantitySetDialogController controller = loader.getController();
      controller.setProductItem(selected, getLimitQuantity(selected));
      dialogBase.createDialogStage(returnPane, openedRoot, controller);
      if (controller.getUpdatedProduct() != null) {
        updateReturnTotal();
        salesReturnTable.refresh();
      }
    } catch (IOException e) {
      log.error("IOException in TransactionReturnController.onQuantityDialog()", e);
      throw new RuntimeException(e);
    }
  }

  @FXML
  public void onReturnDialog() {
    if (cart.isEmpty()) {
      return;
    }
    try {
      var loader = dialogBase.loadFXML("/fxml/return_dialog.fxml");
      BorderPane openedRoot = loader.load();
      ReturnDialogController controller = loader.getController();
      controller.initData(returnPayment, updateReturnTotal());
      dialogBase.createDialogStage(returnPane, openedRoot, controller);
      PaymentSumDetails returnPaymentDetails = controller.getPaymentSumDetails();
      if (returnPaymentDetails != null && controller.isReturnFlag()) {
        paymentReceiptService.processReturnSave(returnPaymentDetails, cart, returnPayment);
        cart.clear();
        returnPayment = null;
        returnSales.clear();
      }
    } catch (IOException e) {
      log.error("IOException in TransactionReturnController.onReturnDialog()", e);
      throw new RuntimeException(e);
    }
  }

  private int getLimitQuantity(ProductItem productItem) {
    return returnSales.stream()
        .filter(sale -> sale.barcode().equals(productItem.getBarcode()))
        .findFirst()
        .map(SalesWithProductName::quantity)
        .orElse(productItem.getQuantity());
  }

  /**
   * Метод вызова при переходе из кнопки Возврат в ReceiptDetailsController
   */
  public void loadReceiptData(PaymentReceipt receipt,
      List<SalesWithProductName> salesWithProductNames) {
    configureUIDuringReturn(receipt, salesWithProductNames);
    cart.addAll(returnSales.stream().map(productMapper::toProductItem).toList());
  }

  private void configureUIDuringReturn(PaymentReceipt receipt,
      List<SalesWithProductName> salesWithProductNames) {
    openedFromReceipt = true;
    withoutPaymentReceiptRadio.setDisable(true);
    withPaymentReceiptRadio.setSelected(true);
    // Прячем поиск по товарам, показываем по чеку
    searchFieldById.setVisible(false);
    searchButton.setVisible(false);
    searchFieldByProduct.setVisible(false);
    receiptPaymentIdLabel.setText(receipt.getId().toString());
    receiptPaymentIdLabel.setVisible(true);
    receiptPaymentDateLabel.setText(formatter.format(receipt.getCreated()));
    receiptPaymentDateLabel.setVisible(true);
    cart.clear();
    returnSales = paymentReceiptService.subtractReturnedSales(receipt, salesWithProductNames);
    returnPayment = receipt;
    configureButton(receipt);
    if (returnSales != null && returnSales.isEmpty()) {
      throw new ValidationException("По чеку все товары были возвращены");
    }
  }

  private void configureButton(PaymentReceipt receipt) {
    if (receipt != null && !receipt.getPaymentType().equals(PaymentType.CASH)) {
      quantityButton.setDisable(true);
      deleteButton.setDisable(true);
    } else {
      quantityButton.setDisable(false);
      deleteButton.setDisable(false);
    }
  }

  /**
   * Метод TabController для сброса состояния по умолчанию, если зашли в таб через UI (не из чека)
   */
  @Override
  public void onTabSelected() {
    if (!openedFromReceipt) {
      // Состояние по умолчанию
      withoutPaymentReceiptRadio.setDisable(false);
      withPaymentReceiptRadio.setSelected(true);
      configureRadioMenu(true);
      receiptPaymentIdLabel.setText("");
      receiptPaymentIdLabel.setVisible(false);
      receiptPaymentDateLabel.setText("");
      receiptPaymentDateLabel.setVisible(false);
      resetValues();
      configureButton(null);
    }
    openedFromReceipt = false; // сбрасываем флаг после применения
  }

  private void resetValues() {
    cart.clear();
    if (returnSales != null && !returnSales.isEmpty()) {
      returnSales.clear();
    }
    returnPayment = null;
  }

  private void configureRadioMenu(boolean flag) {
    searchFieldById.setVisible(flag);
    searchButton.setVisible(flag);
    searchFieldByProduct.setVisible(!flag);
  }
}
