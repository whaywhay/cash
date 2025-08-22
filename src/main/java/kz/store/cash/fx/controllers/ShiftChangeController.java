package kz.store.cash.fx.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import kz.store.cash.fx.component.UiNotificationService;
import kz.store.cash.fx.controllers.lib.TabController;
import kz.store.cash.security.AuthService;
import kz.store.cash.security.CurrentUserStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShiftChangeController implements TabController {
  @FXML private TextField usernameField;
  @FXML private PasswordField passwordField;

  @FXML private GridPane loginPane;
  @FXML private javafx.scene.control.Button loginBtn;
  @FXML private javafx.scene.control.Button logoutBtn;
  @FXML private javafx.scene.control.Label welcomeTitle;

  private final AuthService authService;
  private final CurrentUserStore currentUserStore;
  private final UiNotificationService ui;

  @FXML
  public void initialize() {
    var isAuth = currentUserStore.loggedInBinding();
    loginBtn.setDefaultButton(true);
    loginPane.visibleProperty().bind(isAuth.not());
    loginPane.managedProperty().bind(isAuth.not());
    loginBtn.visibleProperty().bind(isAuth.not());
    loginBtn.managedProperty().bind(isAuth.not());

    // "Выход" и приветствие — ТОЛЬКО после авторизации
    logoutBtn.visibleProperty().bind(isAuth);
    logoutBtn.managedProperty().bind(isAuth);
    welcomeTitle.visibleProperty().bind(isAuth);
    welcomeTitle.managedProperty().bind(isAuth);

    // обновляем текст приветствия при смене пользователя
    currentUserStore.currentUserProperty().addListener((obs, o, u) ->
        welcomeTitle.setText(u != null ? "Вы вошли как: " + u.getDisplayName() : "")
    );
    if (currentUserStore.isLoggedIn()) {
      welcomeTitle.setText("Вы вошли как: " + currentUserStore.get().getDisplayName());
    }
  }

  @FXML
  public void onLogin() {
    try {
      authService.login(usernameField.getText(), passwordField.getText());
      ui.showInfo("Успешный вход: " + currentUserStore.get().getDisplayName());
      usernameField.clear();
      passwordField.clear();
    } catch (Exception e) {
      ui.showError(e.getMessage());
    }
  }

  @FXML
  public void onLogout() {
    authService.logout();
    ui.showInfo("Вы вышли из системы");
  }
}