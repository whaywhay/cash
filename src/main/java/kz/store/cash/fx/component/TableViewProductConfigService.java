package kz.store.cash.fx.component;

import java.util.List;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import kz.store.cash.fx.model.ProductItem;
import org.springframework.stereotype.Component;

@Component
public class TableViewProductConfigService {

  public void configure(TableView<ProductItem> table,
      TableColumn<ProductItem, Boolean> checkboxCol,
      TableColumn<ProductItem, Number> indexCol,
      TableColumn<ProductItem, String> nameCol,
      TableColumn<ProductItem, Number> priceCol,
      TableColumn<ProductItem, Number> qtyCol,
      TableColumn<ProductItem, Number> totalCol,
      ObservableList<ProductItem> cart,
      CheckBox headerCheckBox,
      Runnable updateTotalAndHeaderCheckbox) {

    table.setItems(cart);
    table.setEditable(true);

    checkboxCol.setCellValueFactory(cell -> cell.getValue().selectedProperty());
    checkboxCol.setCellFactory(CheckBoxTableCell.forTableColumn(checkboxCol));
    checkboxCol.setEditable(true);

    indexCol.setCellValueFactory(
        cell -> new ReadOnlyObjectWrapper<>(table.getItems().indexOf(cell.getValue()) + 1));

    nameCol.setCellValueFactory(cell -> cell.getValue().productNameProperty());
    priceCol.setCellValueFactory(cell -> cell.getValue().priceProperty());
    qtyCol.setCellValueFactory(cell -> cell.getValue().quantityProperty());

    totalCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getTotal()));
    totalCol.setCellFactory(column -> new TableCell<>() {
      @Override
      protected void updateItem(Number value, boolean empty) {
        super.updateItem(value, empty);
        if (empty || value == null) {
          setText(null);
        } else {
          setText(String.format("%,.2f", value.doubleValue()));
        }
      }
    });

    // Обновление тотала и чекбокса при изменении списка
    cart.addListener(
        (ListChangeListener<ProductItem>) change -> {
          updateTotalAndHeaderCheckbox.run();
          updateHeaderCheckboxState(cart, headerCheckBox);
        });
    cart.forEach(item -> item.selectedProperty()
        .addListener((obs, oldVal, newVal) -> updateTotalAndHeaderCheckbox.run()));
  }

  private void updateHeaderCheckboxState(List<ProductItem> cart, CheckBox headerCheckBox) {
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
}
