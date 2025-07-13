package kz.store.cash.fx.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import lombok.Setter;

public class NumericKeyboardController {

  @FXML
  private GridPane keyGrid;

  @Setter
  private TextField targetField;
  @Setter
  private Runnable onOkAction;

  public void initialize() {
    String[][] keys = {
        {"7", "8", "9"},
        {"4", "5", "6"},
        {"1", "2", "3"},
        {".", "0", "←"},
        {"OK"}
    };

    for (int row = 0; row < keys.length; row++) {
      for (int col = 0; col < keys[row].length; col++) {
        String key = keys[row][col];
        Button btn = new Button(key);
        btn.setPrefSize(60, 60);
        btn.setStyle("-fx-font-size: 16px;");
        btn.setOnAction(e -> handleKeyPress(key));

        if (key.equals("OK")) {
          GridPane.setColumnSpan(btn, 3);
          keyGrid.add(btn, 0, row);
        } else {
          keyGrid.add(btn, col, row);
        }
      }
    }
  }

  private void handleKeyPress(String key) {
    if (targetField == null) {
      return;
    }

    switch (key) {
      case "OK":
        if (onOkAction != null) {
          onOkAction.run();
        }
        break;
      case "←":
        String current = targetField.getText();
        if (!current.isEmpty()) {
          targetField.setText(current.substring(0, current.length() - 1));
        }
        break;
      default:
        targetField.appendText(key);
        break;
    }
  }

}
