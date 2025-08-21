package kz.store.cash.fx.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import kz.store.cash.fx.component.UiNotificationService;
import kz.store.cash.fx.controllers.lib.TabController;
import kz.store.cash.security.AuthService;
import kz.store.cash.security.CurrentUserStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShiftChangeController implements TabController {
  @FXML
  private TextField usernameField;
  @FXML private PasswordField passwordField;

  private final AuthService authService;
  private final CurrentUserStore currentUserStore;
  private final UiNotificationService ui; // ваш уже существующий сервис уведомлений

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