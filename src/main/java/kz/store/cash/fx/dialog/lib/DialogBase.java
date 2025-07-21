package kz.store.cash.fx.dialog.lib;

import java.io.IOException;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@Getter
@RequiredArgsConstructor
public class DialogBase {

  private double xOffset = 0;
  private double yOffset = 0;
  private final ApplicationContext context;

  public void createDialogStage(Pane rootPane, Pane openedRootPane, Object controller) {

    Stage dialogStage = new Stage();
    Scene openedSceneWindow = new Scene(openedRootPane);
    dialogStage.initOwner(rootPane.getScene().getWindow());
    dialogStage.initStyle(StageStyle.UNDECORATED);
    dialogStage.setScene(openedSceneWindow);
    dialogStage.initModality(Modality.APPLICATION_MODAL);
    openedRootPane.setOnMousePressed(event -> {
      xOffset = event.getSceneX();
      yOffset = event.getSceneY();
    });
    openedRootPane.setOnMouseDragged(event -> {
      dialogStage.setX(event.getScreenX() - xOffset);
      dialogStage.setY(event.getScreenY() - yOffset);
    });
    Platform.runLater(() -> {
      if (controller instanceof CancellableDialog cancelHandler) {
        openedRootPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
          if (event.getCode() == KeyCode.ESCAPE) {
            cancelHandler.handleCancel();
            event.consume();
          }
        });
      }
    });

    dialogStage.showAndWait();
  }

  public FXMLLoader loadFXML(String path) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
    loader.setControllerFactory(context::getBean);
    return loader;
  }

}
