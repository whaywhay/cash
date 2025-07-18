package kz.store.cash.fx.controllers.payments;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.TextField;

public class CardViewController implements HasRoot{

  private Parent root;

  @FXML
  public TextField cardAmountField;


  @Override
  public void setRoot(Parent root) {
    this.root = root;
  }

  @Override
  public Parent getRoot() {
    return root;
  }

}
