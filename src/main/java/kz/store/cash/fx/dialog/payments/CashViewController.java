package kz.store.cash.fx.dialog.payments;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

@Component
public class CashViewController implements HasRoot {

  private Parent root;

  @FXML
  public TextField cashAmountField;

  @Override
  public void setRoot(Parent root) {
    this.root = root;
  }

  @Override
  public Parent getRoot() {
    return root;
  }

}
