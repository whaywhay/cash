package kz.store.cash.fx.controllers;

import static kz.store.cash.model.enums.CashMovementType.IN;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import kz.store.cash.fx.component.UiNotificationService;
import kz.store.cash.fx.controllers.lib.TabController;
import kz.store.cash.model.entity.CashMovement;
import kz.store.cash.model.entity.CashShift;
import kz.store.cash.model.enums.CashMovementType;
import kz.store.cash.model.enums.CashShiftStatus;
import kz.store.cash.security.AuthEvents;
import kz.store.cash.security.AuthService;
import kz.store.cash.security.CurrentUserStore;
import kz.store.cash.service.CashShiftService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShiftChangeController implements TabController {

  @FXML
  private VBox authPane;
  @FXML
  private Label statusLabel;
  @FXML
  private Label openedAtLabel;
  @FXML
  private Label openedByLabel;
  @FXML
  private Label leftInDrawerLabel;
  @FXML
  private TitledPane movementPane;
  @FXML
  private ComboBox<CashMovementType> movementTypeCombo;
  @FXML
  private TextField movementAmountField;
  @FXML
  private TextField movementReasonField;
  @FXML
  private TableView<CashMovement> movementTable;
  @FXML
  private TableColumn<CashMovement, String> colMvTime;
  @FXML
  private TableColumn<CashMovement, String> colMvType;
  @FXML
  private TableColumn<CashMovement, String> colMvAmount;
  @FXML
  private TableColumn<CashMovement, String> colMvReason;
  @FXML
  private TableColumn<CashMovement, String> colMvUser;

  @FXML
  private TitledPane closePane;
  @FXML
  private TextField closingLeftField;
  @FXML
  private TextField closingNoteField;

  private final AuthService authService;
  private final CurrentUserStore currentUserStore;
  private final CashShiftService cashShiftService;
  private final UiNotificationService ui;

  private CashShift currentShift;
  private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @FXML
  public void initialize() {
    BooleanBinding isAuth = Bindings.createBooleanBinding(
        currentUserStore::isLoggedIn, currentUserStore.currentUserProperty());
    authPane.visibleProperty().bind(isAuth);
    authPane.managedProperty().bind(isAuth);

    movementTypeCombo.getItems().setAll(CashMovementType.values());
    movementTypeCombo.getSelectionModel().select(IN);

    colMvTime.setCellValueFactory(cd -> new SimpleStringProperty(fmt(cd.getValue().getCreated())));
    colMvType.setCellValueFactory(
        cd -> new SimpleStringProperty(cd.getValue().getType().getDisplayName()));
    colMvAmount.setCellValueFactory(cd -> new SimpleStringProperty(
        cd.getValue().getAmount() == null ? "" : cd.getValue().getAmount().toPlainString()));
    colMvReason.setCellValueFactory(cd -> new SimpleStringProperty(
        Objects.toString(cd.getValue().getReason(), "")));
    colMvUser.setCellValueFactory(cd -> new SimpleStringProperty(
        cd.getValue().getCreatedByUser() != null ? cd.getValue().getCreatedByUser().getDisplayName()
            : ""));
    currentUserStore.currentUserProperty().addListener((o, oldU, newU) -> {
      if (newU != null) {
        refreshAll();
        Platform.runLater(() -> movementAmountField.requestFocus());
      } else {
        clearShiftUi();
      }
    });

    if (currentUserStore.isLoggedIn()) {
      refreshAll();
    }
  }

  @EventListener
  public void onLogin(AuthEvents.LoginSuccess ev) {
    try {
      cashShiftService.ensureOpenShift(ev.getSource());
    } catch (Exception ignored) {
    }
    Platform.runLater(this::refreshAll);
  }


  @FXML
  public void onAddMovement() {
    ensureAuth();
    BigDecimal amount = parseMoney(movementAmountField.getText());
    CashMovementType type = movementTypeCombo.getValue();
    String reason = trimToNull(movementReasonField.getText());
    try {
      if (amount == null || amount.signum() <= 0) {
        movementAmountField.requestFocus();
        throw new IllegalArgumentException(
            "ВНОС/ВЫНОС СРЕДСТВ не может иметь отрицательное или пустое значение");
      }
      cashShiftService.addMovement(currentShift.getId(), type, amount, reason,
          currentUserStore.get());
      movementAmountField.clear();
      movementReasonField.clear();
      refreshAll();
    } catch (Exception e) {
      ui.showError(e.getMessage());
    }
  }

  @FXML
  public void onCloseShift() {
    ensureAuth();
    BigDecimal left = parseMoney(closingLeftField.getText());
    String note = trimToNull(closingNoteField.getText());
    if (left == null || left.signum() < 0) {
      closingLeftField.requestFocus();
      throw new IllegalArgumentException("Остаток не может быть отрицательным или пустым");
    }
    try {
      cashShiftService.closeShift(currentShift.getId(), left, note, currentUserStore.get());
      ui.showInfo("Смена закрыта");
      authService.logout();
      clearShiftUi();
    } catch (Exception e) {
      ui.showError(e.getMessage());
    }
  }

  /* ====== Helpers ====== */
  private void refreshAll() {
    currentShift = cashShiftService.getOpenedShift().orElse(null);
    boolean open = isShiftOpen();

    movementPane.setVisible(open);
    movementPane.setManaged(open);
    closePane.setVisible(open);
    closePane.setManaged(open);

    statusLabel.setText(open ? "ОТКРЫТА" : "ЗАКРЫТА");
    openedAtLabel.setText(open ? fmt(currentShift.getShiftOpenedDate()) : "—");
    openedByLabel.setText(open && currentShift.getOpenedUser() != null
        ? currentShift.getOpenedUser().getDisplayName() : "—");

    String nowLeftStr = open
        ? currentShift.getCashDuringOpening().toString()
        : "—";
    leftInDrawerLabel.setText(nowLeftStr);
    List<CashMovement> list = open
        ? cashShiftService.findMovements(currentShift.getId())
        : List.of();
    movementTable.getItems().setAll(list);
  }

  private void clearShiftUi() {
    statusLabel.setText("—");
    openedAtLabel.setText("—");
    openedByLabel.setText("—");
    leftInDrawerLabel.setText("—");
    movementTable.getItems().clear();

    movementPane.setVisible(false);
    movementPane.setManaged(false);
    closePane.setVisible(false);
    closePane.setManaged(false);
  }

  private boolean isShiftOpen() {
    return currentShift != null && currentShift.getStatus() == CashShiftStatus.OPENED;
  }

  private void ensureAuth() {
    if (!currentUserStore.isLoggedIn()) {
      throw new IllegalStateException("Не авторизованы");
    }
  }

  private static BigDecimal parseMoney(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim().replace(',', '.');
    if (t.isEmpty()) {
      return null;
    }
    try {
      return new BigDecimal(t);
    } catch (Exception e) {
      return null;
    }
  }

  private static String trimToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private static String fmt(LocalDateTime dt) {
    return (dt == null) ? "" : DTF.format(dt);
  }
}