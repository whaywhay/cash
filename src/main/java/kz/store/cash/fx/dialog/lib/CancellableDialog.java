package kz.store.cash.fx.dialog.lib;

import javafx.scene.Node;
import javafx.stage.Stage;

public interface CancellableDialog {

  void handleClose();

  /**
   * Общая реализация для handleClose(): любой узел этой же сцены годится, чтобы найти и закрыть
   * окно диалога.
   */
  default void closeWindowOf(Node anyNodeInScene) {
    ((Stage) anyNodeInScene.getScene().getWindow()).close();
  }
}
