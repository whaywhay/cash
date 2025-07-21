package kz.store.cash.fx.dialog;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import kz.store.cash.fx.dialog.lib.CancellableDialog;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.util.UtilNumbers;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuantitySetDialogController implements CancellableDialog {

  @FXML
  public TextField quantityField;

  @Getter
  private ProductItem updatedProduct;
  private final UtilNumbers utilNumbers;

  public void setProductItem(ProductItem product) {
    updatedProduct = product;
    this.quantityField.setText(String.valueOf(product.getQuantity()));
    Platform.runLater(() -> {
      utilNumbers.setupIntegerFilter(quantityField, val -> val >= 1 && val <= 100_000);
      quantityField.requestFocus();
    });
  }

  @FXML
  public void onDigitClick(ActionEvent actionEvent) {
    String digit = ((Button) actionEvent.getSource()).getText();
    quantityField.appendText(digit);
  }

  @FXML
  public void onBackspace() {
    String text = quantityField.getText();
    if (!text.isEmpty()) {
      quantityField.setText(text.substring(0, text.length() - 1));
    }
  }

  @FXML
  public void onDotClick() {
  }

  @FXML
  public void onCancel() {
    updatedProduct = null;
    ((Stage) quantityField.getScene().getWindow()).close();
  }

  public void onSave() {
    if (!checkAndInitializeUpdateProduct()) {
      return;
    }
    ((Stage) quantityField.getScene().getWindow()).close();
  }

  private boolean checkAndInitializeUpdateProduct() {
    try {
      int quantity = Integer.parseInt(quantityField.getText());
      if (quantityField.getText().isEmpty() || quantity < 1) {
        return false;
      }
      updatedProduct.setQuantity(quantity);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  @Override
  public void handleCancel() {
    onCancel();
  }
}
