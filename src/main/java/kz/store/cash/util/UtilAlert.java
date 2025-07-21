package kz.store.cash.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import org.springframework.stereotype.Component;

@Component
public class UtilAlert {

  public void showError(String title, String msg) {
    Alert alert = new Alert(AlertType.ERROR, msg);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.showAndWait();
  }

  public void showInfo(String title, String msg) {
    Alert alert = new Alert(AlertType.INFORMATION, msg);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.showAndWait();
  }
}
