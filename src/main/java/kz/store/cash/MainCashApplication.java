package kz.store.cash;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class MainCashApplication extends Application {

  private ConfigurableApplicationContext context;

  @Override
  public void init() {
    // Запускаем Spring Context
    context = new SpringApplicationBuilder(CashApplication.class).run();
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
    fxmlLoader.setControllerFactory(context::getBean);

    Parent root = fxmlLoader.load();

    primaryStage.setTitle("YelCashStore");
    primaryStage.setScene(new Scene(root));
    primaryStage.setFullScreen(true);
    primaryStage.show();
  }

  @Override
  public void stop() {
    System.out.println("stop: stopping");
    context.close();
    Platform.exit();
    System.out.println("stop: exiting");
  }



  public static void main(String[] args) {
    launch(args);
  }
}
