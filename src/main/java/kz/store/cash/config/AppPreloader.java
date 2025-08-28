package kz.store.cash.config;

import javafx.application.Preloader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AppPreloader extends Preloader {

  private Stage stage;
  private final ProgressBar bar = new ProgressBar(0);
  private final Label status = new Label("Инициализация…");

  @Override
  public void start(Stage primaryStage) {
    this.stage = primaryStage;
    VBox box = new VBox(12, status, bar);
    box.setAlignment(Pos.CENTER);
    box.setPrefSize(360, 180);
    Scene scene = new Scene(box);
    stage.initStyle(StageStyle.UNDECORATED);
    stage.setScene(scene);
    stage.setAlwaysOnTop(true);
    stage.show();
  }

  @Override
  public void handleApplicationNotification(PreloaderNotification info) {
    if (info instanceof ProgressNotification pn) {
      double p = Math.max(0, Math.min(1, pn.getProgress()));
      bar.setProgress(p);
      status.setText("Загрузка… " + (int) (p * 100) + "%");
    }
  }

  @Override
  public void handleStateChangeNotification(StateChangeNotification info) {
    if (info.getType() == StateChangeNotification.Type.BEFORE_START) {
      if (stage != null) stage.hide(); // или stage.close();
    }
  }
}
