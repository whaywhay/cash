package kz.store.cash.fx.dialog.lib;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import kz.store.cash.fx.model.LocationSize;
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

  public void createDialogStage(Node rootPane, Pane openedRootPane, Object controller) {
    Stage dialogStage = processDialogStage(rootPane, openedRootPane, controller);
    dialogStage.showAndWait();
  }

  public void createWithLocationDialogStage(Pane rootPane, Pane openedRootPane, Object controller,
      LocationSize locationSize) {
    Stage dialogStage = processDialogStage(rootPane, openedRootPane, controller);
    dialogStage.setX(locationSize.locationX());
    dialogStage.setY(locationSize.locationY());
    dialogStage.setWidth(locationSize.width());
    dialogStage.setHeight(locationSize.height());
    dialogStage.showAndWait();
  }

  private Stage processDialogStage(Node rootPane, Pane openedRootPane, Object controller) {
    Stage dialogStage = new Stage();
    Scene openedSceneWindow = new Scene(openedRootPane);
    dialogStage.initOwner(rootPane.getScene().getWindow());
    dialogStage.initStyle(StageStyle.UNDECORATED);
    dialogStage.setScene(openedSceneWindow);
    dialogStage.initModality(Modality.APPLICATION_MODAL);
    moveDialogScreen(dialogStage, openedRootPane);
    screenEscapeAble(controller, openedRootPane);
    return dialogStage;
  }

  public void createFullscreenDialogStage(Pane rootPane, Pane openedRootPane, Object controller) {
    Stage owner =
        (rootPane != null && rootPane.getScene() != null) ?
            (Stage) rootPane.getScene().getWindow() : null;
    Stage stage = new Stage();
    if (owner != null) {
      stage.initOwner(owner);
    }
    stage.initModality(Modality.APPLICATION_MODAL);
    stage.initStyle(StageStyle.UNDECORATED);
    Scene scene = new Scene(openedRootPane);
    stage.setScene(scene);
    stage.setMaximized(true);
    moveDialogScreen(stage, openedRootPane);
    screenEscapeAble(controller, openedRootPane);
    stage.showAndWait();
  }

  public FXMLLoader loadFXML(String path) {
    FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
    loader.setControllerFactory(context::getBean);
    return loader;
  }

  //can move dialog screen
  private void moveDialogScreen(Stage dialogStage, Pane openedRootPane) {
    openedRootPane.setOnMousePressed(e -> {
      xOffset = e.getSceneX();
      yOffset = e.getSceneY();
    });
    openedRootPane.setOnMouseDragged(e -> {
      dialogStage.setX(e.getScreenX() - xOffset);
      dialogStage.setY(e.getScreenY() - yOffset);
    });
  }

  private void screenEscapeAble(Object controller, Pane openedRootPane) {
    Platform.runLater(() -> {
      if (controller instanceof CancellableDialog cancelHandler) {
        openedRootPane.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
          if (event.getCode() == KeyCode.ESCAPE) {
            cancelHandler.handleClose();
            event.consume();
          }
        });
      }
    });
  }

}
