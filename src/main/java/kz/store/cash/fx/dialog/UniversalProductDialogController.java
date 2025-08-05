package kz.store.cash.fx.dialog;

import static kz.store.cash.util.UtilNumbers.parseDoubleAmount;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import kz.store.cash.fx.dialog.lib.CancellableDialog;
import kz.store.cash.util.UtilNumbers;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UniversalProductDialogController implements CancellableDialog {

  @FXML
  public TextField priceField;

  @Getter
  private double priceUniversalProduct;
  private final UtilNumbers utilNumbers;

  public void initData() {
    Platform.runLater(() -> {
      utilNumbers.setupDecimalFilter(priceField);
      priceField.requestFocus();
    });
  }

  @FXML
  public void onDigitClick(ActionEvent actionEvent) {
    String digit = ((Button) actionEvent.getSource()).getText();
    priceField.appendText(digit);
  }

  @FXML
  public void onBackspace() {
    String text = priceField.getText();
    if (!text.isEmpty()) {
      priceField.setText(text.substring(0, text.length() - 1));
    }
  }

  @FXML
  public void onDotClick() {
    if (!priceField.getText().contains(".")) {
      priceField.appendText(".");
    }
  }

  @FXML
  public void onCancel() {
    priceUniversalProduct = 0;
    handleClose();
  }

  public void onSave() {
    if (!checkAndInitializePrice()) {
      return;
    }
    handleClose();
  }

  private boolean checkAndInitializePrice() {
    try {
      double price = parseDoubleAmount(priceField.getText());
      if (priceField.getText().isEmpty() || price < 0) {
        return false;
      }
      priceUniversalProduct = price;
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  @Override
  public void handleClose() {
    ((Stage) priceField.getScene().getWindow()).close();
  }
}
