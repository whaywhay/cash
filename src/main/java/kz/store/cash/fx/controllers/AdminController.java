package kz.store.cash.fx.controllers;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import kz.store.cash.fx.component.UiNotificationService;
import kz.store.cash.handler.ValidationException;
import kz.store.cash.model.UserDto;
import kz.store.cash.model.entity.AppSetting;
import kz.store.cash.model.entity.User;
import kz.store.cash.model.enums.UserRole;
import kz.store.cash.service.AppSettingService;
import kz.store.cash.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminController {

  // --- Users tab UI
  @FXML
  private TableView<User> userTable;
  @FXML
  private TableColumn<User, Number> colUserId;
  @FXML
  private TableColumn<User, String> colUsername;
  @FXML
  private TableColumn<User, String> colDisplayName;
  @FXML
  private TableColumn<User, String> colRole;
  @FXML
  private TableColumn<User, Boolean> colActive;
  @FXML
  private TextField searchUserField;
  @FXML
  private TextField userIdField;
  @FXML
  private TextField usernameField;
  @FXML
  private TextField displayNameField;
  @FXML
  private ComboBox<UserRole> roleCombo;
  @FXML
  private PasswordField passwordField;
  @FXML
  private PasswordField password2Field;
  @FXML
  private CheckBox activeCheck;
  @FXML
  private TextField orgNameField;
  @FXML
  private TextField binField;
  @FXML
  private TextField addressField;
  @FXML
  private TextField saleStoreField;

  private final UserService userService;
  private final AppSettingService appSettingService;
  private final UiNotificationService ui;

  private AppSetting currentSetting;

  @FXML
  public void initialize() {
    // таблица пользователей
    colUserId.setCellValueFactory(cd -> new SimpleLongProperty(cd.getValue().getId()));
    colUsername.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getUsername()));
    colDisplayName.setCellValueFactory(
        cd -> new SimpleStringProperty(cd.getValue().getDisplayName()));
    colRole.setCellValueFactory(
        cd -> new SimpleStringProperty(String.valueOf(cd.getValue().getRole())));
    colActive.setCellValueFactory(cd ->
        new ReadOnlyBooleanWrapper(cd.getValue().isActive()));
    colActive.setCellFactory(CheckBoxTableCell.forTableColumn(colActive));
    userTable.getSelectionModel().selectedItemProperty()
        .addListener((obs, o, n) -> fillUserForm(n));
    roleCombo.getItems().setAll(UserRole.values());
    refreshUsers();
    loadSettings();
  }

  /* ===================== Users Configuration ===================== */

  @FXML
  public void onRefreshUsers() {
    refreshUsers();
  }

  @FXML
  public void onSearchUsers() {
    String searchFieldText = trimToNull(searchUserField.getText());
    if (searchFieldText == null) {
      refreshUsers();
      return;
    }
    final String search = searchFieldText.toLowerCase();
    List<User> filtered = userService.findAll().stream()
        .filter(user -> {
          String un = Objects.toString(user.getUsername(), "").toLowerCase();
          String dn = Objects.toString(user.getDisplayName(), "").toLowerCase();
          return un.contains(search) || dn.contains(search);
        })
        .collect(Collectors.toList());
    userTable.getItems().setAll(filtered);
  }

  private void refreshUsers() {
    userTable.getItems().setAll(userService.findAll());
  }

  private void fillUserForm(User user) {
    if (user == null) {
      clearUserForm();
      return;
    }
    userIdField.setText(String.valueOf(user.getId()));
    usernameField.setText(user.getUsername());
    displayNameField.setText(user.getDisplayName());
    roleCombo.getSelectionModel().select(user.getRole());
    activeCheck.setSelected(user.isActive());
    passwordField.clear();
    password2Field.clear();
  }

  private void clearUserForm() {
    userIdField.clear();
    usernameField.clear();
    displayNameField.clear();
    roleCombo.getSelectionModel().clearSelection();
    activeCheck.setSelected(true);
    passwordField.clear();
    password2Field.clear();
    userTable.getSelectionModel().clearSelection();
  }

  private boolean validateUserForm(boolean isCreate) {
    if (usernameField.getText() == null || usernameField.getText().isBlank()) {
      throw new ValidationException("Логин обязателен");
    }
    if (displayNameField.getText() == null || displayNameField.getText().isBlank()) {
      throw new ValidationException("Имя (отображаемое) обязательно");
    }
    if (roleCombo.getValue() == null) {
      throw new ValidationException("Выберите роль");
    }
    if (isCreate) {
      if (passwordField.getText() == null || passwordField.getText().isBlank()) {
        throw new ValidationException("Пароль обязателен для нового пользователя");
      }
    }
    if (!Objects.equals(passwordField.getText(), password2Field.getText())) {
      throw new ValidationException("Пароли не совпадают");
    }
    return true;
  }

  @FXML
  public void onNewUser() {
    clearUserForm();
    roleCombo.getSelectionModel().select(UserRole.CASHIER);
    activeCheck.setSelected(true);
  }

  @FXML
  public void onSaveUser() {
    boolean isCreate = userIdField.getText() == null || userIdField.getText().isBlank();
    if (!validateUserForm(isCreate)) {
      return;
    }
    try {
      UserDto userDto = createUserDto();
      if (isCreate) {
        userService.createUser(userDto);
        ui.showInfo("Пользователь создан");
      } else {
        userService.updateUser(userDto);
        ui.showInfo("Пользователь обновлён");
      }
      refreshUsers();
      clearUserForm();
    } catch (Exception e) {
      log.error("Ошибка при сохранении пользователя", e);
      ui.showError(e.getMessage());
    }
  }

  private UserDto createUserDto() {
    Long id = Optional.ofNullable(userIdField.getText())
        .map(String::trim)
        .filter(s -> !s.isEmpty() && s.chars().allMatch(Character::isDigit))
        .map(Long::valueOf)
        .orElse(null);             // безопасно
    String username = trimToNull(usernameField.getText());
    String displayName = trimToNull(displayNameField.getText());
    String password = trimToNull(passwordField.getText());            // null если пусто
    UserRole role = roleCombo.getValue();
    Boolean active = activeCheck.isSelected();
    return new UserDto(id, username, password, displayName, role, active);
  }

  @FXML
  public void onDeleteUser() {
    User selectedUser = userTable.getSelectionModel().getSelectedItem();
    if (selectedUser == null) {
      throw new ValidationException("Выберите пользователя для удаления");
    }
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
        "Удалить пользователя " + selectedUser.getUsername() + "?", ButtonType.OK, ButtonType.CANCEL);
    alert.setHeaderText(null);
    alert.showAndWait().ifPresent(btn -> {
      if (btn == ButtonType.OK) {
        try {
          userService.deleteUser(selectedUser.getId());
          ui.showInfo("Удалён");
          refreshUsers();
          clearUserForm();
        } catch (Exception e) {
          log.error("Ошибка при удалении пользователя", e);
          ui.showError(e.getMessage());
        }
      }
    });
  }

  /* ===================== Settings Organization ===================== */

  private void loadSettings() {
    currentSetting = appSettingService.loadOrNew();
    orgNameField.setText(nvl(currentSetting.getOrgName()));
    binField.setText(nvl(currentSetting.getBin()));
    addressField.setText(nvl(currentSetting.getAddress()));
    saleStoreField.setText(nvl(currentSetting.getSaleStore()));
  }

  @FXML
  public void onReloadSettings() {
    loadSettings();
    ui.showInfo("Настройки обновлены");
  }

  @FXML
  public void onSaveSettings() {
    try {
      currentSetting.setOrgName(trimOrNull(orgNameField.getText()));
      currentSetting.setBin(trimOrNull(binField.getText()));
      currentSetting.setAddress(trimOrNull(addressField.getText()));
      currentSetting.setSaleStore(trimOrNull(saleStoreField.getText()));
      currentSetting = appSettingService.saveSingleton(currentSetting);
      ui.showInfo("Настройки сохранены");
    } catch (Exception e) {
      log.error("Ошибка при сохранении настроек по организации", e);
      ui.showError(e.getMessage());
    }
  }

  private static String nvl(String s) {
    return s == null ? "" : s;
  }

  private static String trimOrNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private static String trimToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}