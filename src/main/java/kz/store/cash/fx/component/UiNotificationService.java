package kz.store.cash.fx.component;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Component
public class UiNotificationService {

  @Setter
  private Stage primaryStage;

  private final VBox notificationContainer = new VBox();
  private Stage notificationStage;
  private Timeline hideTimeline;

  public void showBusinessError(String message) {
    showNotification(message, "#FF9800");
  }

  public void showInfo(String message) {
    showNotification(message, "#388E3C");
  }

  public void showValidationError(String message) {
    showNotification(message, "#1976D2");
  }

  public void showError(String message) {
    showNotification(message, "#D32F2F");
  }

  private void showNotification(String message, String bgColor) {
    Platform.runLater(() -> {
      if (notificationStage == null) {
        initNotificationStage();
      }
      // Очистить старое сообщение
      notificationContainer.getChildren().clear();
      // Создать новое уведомление
      StackPane box = createNotificationBox(message, bgColor);
      notificationContainer.getChildren().add(box);
      // Остановить предыдущий таймер, если был
      if (hideTimeline != null) {
        hideTimeline.stop();
      }
      // Авто-скрытие через 3 секунды
      hideTimeline = new Timeline(
          new KeyFrame(Duration.seconds(3), ev -> notificationStage.hide()));
      hideTimeline.setCycleCount(1);
      hideTimeline.play();

      updatePosition();
      if (!notificationStage.isShowing()) {
        notificationStage.show();
      }
    });
  }

  private void initNotificationStage() {
    notificationStage = new Stage(StageStyle.TRANSPARENT);
    notificationStage.setAlwaysOnTop(true);
    notificationStage.initOwner(primaryStage);

    notificationContainer.setAlignment(Pos.TOP_RIGHT);
    notificationContainer.setStyle("-fx-padding: 15;");

    Scene scene = new Scene(notificationContainer);
    scene.setFill(null); // прозрачный фон
    notificationStage.setScene(scene);

    // Слушаем перемещение окна
    primaryStage.xProperty().addListener((obs, o, n) -> updatePosition());
    primaryStage.yProperty().addListener((obs, o, n) -> updatePosition());
    primaryStage.widthProperty().addListener((obs, o, n) -> updatePosition());
  }

  private void updatePosition() {
    if (primaryStage != null && notificationStage != null) {
      notificationStage.setX(primaryStage.getX() + primaryStage.getWidth() - 340);
      notificationStage.setY(primaryStage.getY() + 20);
    }
  }

  private StackPane createNotificationBox(String message, String bgColor) {
    Label label = new Label(message);
    label.setStyle("""
            -fx-text-fill: white;
            -fx-font-size: 16px;
            -fx-wrap-text: true;
        """);

    StackPane box = new StackPane(label);
    box.setPrefWidth(320);
    box.setStyle(String.format("""
            -fx-background-color: %s;
            -fx-background-radius: 6;
            -fx-padding: 12;
            -fx-border-radius: 6;
            -fx-border-color: white;
            -fx-border-width: 1;
        """, bgColor));

    return box;
  }
}