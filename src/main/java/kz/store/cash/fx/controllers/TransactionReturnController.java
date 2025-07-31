package kz.store.cash.fx.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import kz.store.cash.fx.component.SalesCartService;
import kz.store.cash.fx.component.TableViewProductConfigService;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.util.TableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionReturnController {


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

  @FXML
  private ToggleGroup toggleGroup;

  private final ObservableList<ProductItem> cart = FXCollections.observableArrayList();
  private final SalesCartService salesCartService;
  private final TableViewProductConfigService tableViewProductConfigService;

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
    toggleGroup = new ToggleGroup();
    withPaymentReceiptRadio.setToggleGroup(toggleGroup);
    withoutPaymentReceiptRadio.setToggleGroup(toggleGroup);

    // Состояние по умолчанию
    withPaymentReceiptRadio.setSelected(true);
    searchFieldById.setVisible(true);

    // Слушатель для изменения видимости поля поиска
    toggleGroup.selectedToggleProperty().addListener((obs, old, selected) -> {
      boolean isWithReceipt = selected == withPaymentReceiptRadio;
      searchFieldById.setVisible(isWithReceipt);
      searchFieldByProduct.setVisible(!isWithReceipt);
    });
  }

  public void initTableView() {
    tableViewProductConfigService.configure(salesReturnTable, checkboxCol, indexCol, nameCol,
        priceCol, qtyCol, totalCol, cart, headerCheckBox,
        this::updateReturnTotal
    );
  }

  private void updateReturnTotal() {
    double total = salesCartService.calculateTotal(cart);
    returnSumLabel.setText(String.format("%.2f тг", total));
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
    }
  }

  @FXML
  public void onReturnDialog() {
  }
}
