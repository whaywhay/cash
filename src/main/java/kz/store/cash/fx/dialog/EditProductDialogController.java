package kz.store.cash.fx.dialog;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import kz.store.cash.fx.dialog.lib.CancellableDialog;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.service.ProductService;
import kz.store.cash.util.UtilNumbers;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EditProductDialogController implements CancellableDialog {

  @FXML
  private TextField productName;
  @FXML
  private TextField retailPriceField;
  @FXML
  private TextField wholesalePriceField;

  @Getter
  private ProductItem updatedProduct;

  private final ProductService productService;
  private final UtilNumbers utilNumbers;

  public void setProductItem(ProductItem product) {
    this.updatedProduct = product;
    productName.setText(product.getProductName());
    retailPriceField.setText(UtilNumbers.formatDouble(product.getOriginalPrice()));
    wholesalePriceField.setText(UtilNumbers.formatDouble(product.getWholesalePrice()));
    initEvent();
  }

  public void initEvent() {
    Platform.runLater(() -> {
      utilNumbers.setupDecimalFilter(retailPriceField);
      retailPriceField.requestFocus();
    });
  }

  @FXML
  private void onDigitClick(ActionEvent e) {
    String digit = ((Button) e.getSource()).getText();
    retailPriceField.appendText(digit);
  }

  @FXML
  private void onDotClick() {
    if (!retailPriceField.getText().contains(".")) {
      retailPriceField.appendText(".");
    }
  }

  @FXML
  private void onBackspace() {
    String text = retailPriceField.getText();
    if (!text.isEmpty()) {
      retailPriceField.setText(text.substring(0, text.length() - 1));
    }
  }

  @FXML
  private void onCancel() {
    updatedProduct = null;
    ((Stage) productName.getScene().getWindow()).close();
  }

  @FXML
  private void onSave() {
    if (checkAndInitializeUpdateProduct()) {
      productService.updateRetailPrice(updatedProduct.getBarcode(), updatedProduct.getPrice());
      ((Stage) productName.getScene().getWindow()).close();
    }
  }

  @FXML
  private void onApply() {
    if (checkAndInitializeUpdateProduct()) {
      ((Stage) productName.getScene().getWindow()).close();
    }
  }

  private boolean checkAndInitializeUpdateProduct() {
    try {
      double price = Double.parseDouble(retailPriceField.getText());
      if (!retailPriceField.getText().trim().isEmpty()
          && price > 0
          && price >= updatedProduct.getWholesalePrice()) {
        updatedProduct.setPrice(price);
        return true;
      }
      return false;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  @Override
  public void handleCancel() {
    onCancel();
  }
}