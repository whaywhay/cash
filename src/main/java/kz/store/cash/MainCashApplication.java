package kz.store.cash;


import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javax.swing.JOptionPane;
import kz.store.cash.fx.component.UiNotificationService;
import kz.store.cash.handler.GlobalExceptionHandler;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class MainCashApplication extends Application {

  static final Path LOCK_FILE = Path.of("cash-store.lock");

  private static FileChannel lockChannel;
  private static FileLock lock;

  private ConfigurableApplicationContext context;

  @Override
  public void init() {
    notifyPreloader(new Preloader.ProgressNotification(0.9));
    context = new SpringApplicationBuilder(CashApplication.class).run();
    notifyPreloader(new Preloader.ProgressNotification(1.0));
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    UiNotificationService notificationService = context.getBean(UiNotificationService.class);
    notificationService.setPrimaryStage(primaryStage);

    GlobalExceptionHandler exceptionHandler = context.getBean(GlobalExceptionHandler.class);
    Thread.setDefaultUncaughtExceptionHandler(
        (thread, throwable) -> exceptionHandler.handleJavaFxException(throwable));

    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
    fxmlLoader.setControllerFactory(context::getBean);

    Parent root = fxmlLoader.load();
    Scene scene = new Scene(root);
    scene.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.F12) {
        primaryStage.setFullScreen(true);
      }
    });
    primaryStage.setTitle("Cash Store (SQLite)");
    primaryStage.setScene(scene);
    primaryStage.setFullScreen(true);
    primaryStage.show();
  }

  @Override
  public void stop() {
    context.close();
    releaseSingleInstanceLock();
    Platform.exit();
  }

  /**
   * SQLite — однопользовательская база с одним писателем (см. app.http/README про нагрузку):
   * второй запущенный экземпляр этой же кассы на том же ПК будет драться за файл БД и получать
   * "database is locked". Файловая блокировка ловит это ДО старта Spring/JavaFX, а не после
   * первой же ошибки записи.
   */
  static boolean acquireSingleInstanceLock() {
    FileChannel channel = null;
    try {
      channel = FileChannel.open(LOCK_FILE, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
      FileLock acquired = channel.tryLock();
      if (acquired == null) {
        closeQuietly(channel);
        return false;
      }
      lockChannel = channel;
      lock = acquired;
      return true;
    } catch (IOException | OverlappingFileLockException e) {
      // Канал успели открыть, но лок не достался (или упал сам open) — не трогаем ранее
      // удержанный валидный lockChannel/lock, если он есть, просто закрываем этот неудачный.
      closeQuietly(channel);
      return false;
    }
  }

  static void releaseSingleInstanceLock() {
    try {
      if (lock != null) {
        lock.release();
      }
    } catch (IOException ignored) {
      // приложение и так завершается — ОС снимет блокировку сама
    }
    closeQuietly(lockChannel);
    lock = null;
    lockChannel = null;
  }

  private static void closeQuietly(FileChannel channel) {
    if (channel != null) {
      try {
        channel.close();
      } catch (IOException ignored) {
      }
    }
  }

  public static void main(String[] args) {
    if (!acquireSingleInstanceLock()) {
      JOptionPane.showMessageDialog(null,
          "Cash Store уже запущен. Закройте предыдущий запущенный экземпляр программы.",
          "Cash Store", JOptionPane.WARNING_MESSAGE);
      return;
    }
    launch(args);
  }
}
