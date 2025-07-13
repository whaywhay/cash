package kz.store.cash.fx.dialog;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import kz.store.cash.fx.controllers.NumericKeyboardController;

public class NumericKeyboardDialog {

  public static void show(TextField targetField, Runnable onOkAction) {
    try {
      FXMLLoader loader = new FXMLLoader(
          NumericKeyboardDialog.class.getResource("/fxml/numeric_keyboard.fxml"));
      Parent root = loader.load();

      NumericKeyboardController controller = loader.getController();
      controller.setTargetField(targetField);
      controller.setOnOkAction(() -> {
        if (onOkAction != null) {
          onOkAction.run();
        }
        ((Stage) root.getScene().getWindow()).close();
      });

      Stage dialog = new Stage();
      dialog.initModality(Modality.APPLICATION_MODAL);
      dialog.setTitle("Экранная клавиатура");
      dialog.setScene(new Scene(root));
      dialog.showAndWait();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}