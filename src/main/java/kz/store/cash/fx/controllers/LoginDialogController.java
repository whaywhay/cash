package kz.store.cash.fx.controllers;

import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import kz.store.cash.fx.component.UiNotificationService;
import kz.store.cash.model.entity.User;
import kz.store.cash.security.AuthService;
import kz.store.cash.service.UserService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginDialogController {

  @FXML
  private ComboBox<String> userCombo;
  @FXML
  private PasswordField passwordField;
  @FXML
  private Button btnLogin;
  @Getter
  private boolean exit = false;
  @Getter
  private boolean authenticated = false;
  private final UserService userService;
  private final AuthService authService;
  private final UiNotificationService ui;


  @FXML
  public void initialize() {
    exit = false;
    var usersName = userService.findAll().stream()
        .filter(User::isActive)
        .sorted((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()))
        .map(User::getUsername)
        .collect(Collectors.toList());
    userCombo.getItems().setAll(usersName);
    if (!usersName.isEmpty()) {
      userCombo.getSelectionModel().select(0);
    }

    Platform.runLater(() -> {
      if (userCombo.getValue() == null) {
        userCombo.requestFocus();
      } else {
        passwordField.requestFocus();
      }
    });
  }

  @FXML
  public void onDigitClick(javafx.event.ActionEvent e) {
    var btn = (Button) e.getSource();
    passwordField.appendText(btn.getText());
  }

  @FXML
  public void onBackspace() {
    var t = passwordField.getText();
    if (t != null && !t.isEmpty()) {
      passwordField.setText(t.substring(0, t.length() - 1));
    }
  }

  @FXML
  public void onLogin() {
    var userName = userCombo.getValue();
    if (userName == null) {
      ui.showError("Выберите пользователя");
      return;
    }
    try {
      authService.login(userName, passwordField.getText());
      authenticated = true;
      close();
    } catch (Exception ex) {
      ui.showError(ex.getMessage());
      passwordField.clear();
      passwordField.requestFocus();
    }
  }

  @FXML
  public void onExit() {
    authenticated = false;
    exit = true;
    close();
  }

  private void close() {
    Stage stage = (Stage) btnLogin.getScene().getWindow();
    stage.close();
  }
}
