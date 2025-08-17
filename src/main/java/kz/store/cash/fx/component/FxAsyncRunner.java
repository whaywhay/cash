package kz.store.cash.fx.component;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import kz.store.cash.handler.GlobalExceptionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FxAsyncRunner {

  private final GlobalExceptionHandler exceptionHandler;

  // --------- ПУБЛИЧНЫЕ API ---------

  /**
   * Runnable-версия (без результата).
   */
  public void runWithLoader(Node uiBlocker, String message, Runnable action) {
    runWithLoader(uiBlocker, message, 300, asCallable(action), null);
  }

  /**
   * Runnable-версия c настраиваемой задержкой показа лоадера (мс).
   */
  public void runWithLoader(Node uiBlocker, String message, long showDelayMillis, Runnable action) {
    runWithLoader(uiBlocker, message, showDelayMillis, asCallable(action), null);
  }

  /**
   * Callable-версия (с результатом и onSuccess-коллбеком).
   */
  public <T> void runWithLoader(Node uiBlocker, String message,
      Callable<T> action, Consumer<T> onSuccess) {
    runWithLoader(uiBlocker, message, 300, action, onSuccess);
  }

  /**
   * Callable-версия с кастомной задержкой показа лоадера.
   */
  public <T> void runWithLoader(Node uiBlocker, String message, long showDelayMillis,
      Callable<T> action, Consumer<T> onSuccess) {
    final Window owner = getOwner(uiBlocker);
    final Stage progress = createProgressDialog(owner, message);

    // блокируем выбранный узел интерфейса
    final Cursor prevCursor = uiBlocker.getCursor();
    uiBlocker.setDisable(true);
    uiBlocker.setCursor(Cursor.WAIT);

    Task<T> task = new Task<>() {
      @Override
      protected T call() throws Exception {
        return action.call();
      }
    };

    // показываем лоадер только если операция длится дольше порога
    PauseTransition delay = new PauseTransition(Duration.millis(Math.max(0, showDelayMillis)));
    delay.setOnFinished(e -> {
      if (task.isRunning()) {
        progress.show();
      }
    });

    Runnable finish = () -> {
      delay.stop();
      progress.close();
      uiBlocker.setDisable(false);
      uiBlocker.setCursor(prevCursor);
    };

    task.setOnSucceeded(e -> {
      finish.run();
      if (onSuccess != null) {
        onSuccess.accept(task.getValue());
      }
    });

    task.setOnFailed(e -> {
      Throwable ex = task.getException();
      finish.run();
      exceptionHandler.handleJavaFxException(ex);
    });

    delay.playFromStart();

    Thread t = new Thread(task, "fx-async");
    t.setDaemon(true);
    t.start();
  }

  // --------- ВНУТРЕННЕЕ ---------

  private static Callable<Void> asCallable(Runnable r) {
    return () -> {
      r.run();
      return null;
    };
  }

  private static Window getOwner(Node node) {
    Scene scene = node == null ? null : node.getScene();
    return scene == null ? null : scene.getWindow();
  }

  private static Stage createProgressDialog(Window owner, String text) {
    ProgressIndicator pi = new ProgressIndicator();
    Label label = new Label(text);
    VBox box = new VBox(12, pi, label);
    box.setAlignment(Pos.CENTER);
    box.setStyle("-fx-padding: 24;");

    Stage stage = new Stage(StageStyle.UNDECORATED); // без крестика
    if (owner != null) {
      stage.initOwner(owner);
    }
    stage.initModality(Modality.APPLICATION_MODAL);
    stage.setResizable(false);
    stage.setTitle("Пожалуйста, подождите");
    stage.setScene(new Scene(box));
    stage.setOnCloseRequest(Event::consume); // блокируем закрытие (Alt+F4/ESC)
    return stage;
  }
}
