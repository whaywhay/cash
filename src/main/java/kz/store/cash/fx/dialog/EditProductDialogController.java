package kz.store.cash.fx.dialog;

import static kz.store.cash.util.UtilNumbers.parseDoubleAmount;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import kz.store.cash.config.ProductProperties;
import kz.store.cash.fx.dialog.lib.CancellableDialog;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.handler.ValidationException;
import kz.store.cash.service.ProductService;
import kz.store.cash.util.UtilNumbers;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EditProductDialogController implements CancellableDialog {

  @FXML
  public Button savePriceButton;
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
  private final ProductProperties properties;

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
      if (updatedProduct.getBarcode().equals(properties.universalProductBarcode())) {
        savePriceButton.setDisable(true);
      }
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
    handleClose();
  }

  @FXML
  private void onSave() {
    if (checkAndInitializeUpdateProduct()) {
      productService.updateRetailPrice(updatedProduct.getBarcode(), updatedProduct.getPrice());
      handleClose();
    } else {
      throw new ValidationException("Розничная цена должна быть больше чем оптовая цена");
    }
  }

  @FXML
  private void onApply() {
    if (checkAndInitializeUpdateProduct()) {
      ((Stage) productName.getScene().getWindow()).close();
    } else {
      throw new ValidationException("Розничная цена должна быть больше чем оптовая цена");
    }
  }

  private boolean checkAndInitializeUpdateProduct() {
    try {
      double price = parseDoubleAmount(retailPriceField.getText());
      if (!retailPriceField.getText().trim().isEmpty()
          && price > 0
          && price >= updatedProduct.getWholesalePrice()) {
        updatedProduct.setPrice(price);
        return true;
      }
      return false;
    } catch (NumberFormatException e) {
      log.error("checkAndInitializeUpdateProduct: NumberFormatException", e);
      return false;
    }
  }

  @Override
  public void handleClose() {
    ((Stage) productName.getScene().getWindow()).close();
  }
}