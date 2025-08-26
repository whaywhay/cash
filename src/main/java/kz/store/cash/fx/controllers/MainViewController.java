package kz.store.cash.fx.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import kz.store.cash.fx.component.UiNotificationService;
import kz.store.cash.fx.dialog.lib.DialogBase;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.fx.controllers.lib.TabController;
import kz.store.cash.fx.model.SalesWithProductName;
import kz.store.cash.model.enums.UserRole;
import kz.store.cash.security.AuthEvents;
import kz.store.cash.service.CashShiftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MainViewController {

  private final ConfigurableApplicationContext context;
  private final CashShiftService cashShiftService;
  private final DialogBase dialogBase;
  private final UiNotificationService uiNotificationService;

  @FXML
  public TabPane tabPane;
  @FXML
  public Tab salesTab;
  @FXML
  public Tab shiftChangeTab;
  @FXML
  public Tab salesHistoryTab;
  @FXML
  public Tab returnTab;
  @FXML
  public Tab adminTab;

  private final Map<Tab, Node> tabContentCache = new HashMap<>();
  private final Map<Tab, Object> tabControllerCache = new HashMap<>();

  private boolean loggedIn = false;
  private boolean isAdmin = false;

  @FXML
  public void initialize() {
    applyAuthState(false, false);

    tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
      if (newTab != null && newTab.isDisable()) {
        tabPane.getSelectionModel().select(shiftChangeTab);
        return;
      }
      tabInitialize(newTab);
    });

    tabPane.getSelectionModel().select(shiftChangeTab);
    tabInitialize(shiftChangeTab);

    // При старте — показываем диалог и повторяем, пока не войдут
    Platform.runLater(this::showLoginDialogLoop);
  }

  /**
   * Показать диалог логина и повторять до успешной аутентификации.
   */
  private void showLoginDialogLoop() {
    boolean exit = false;
    while (!loggedIn) {
      try {
        var loader = dialogBase.loadFXML("/fxml/login_dialog.fxml");
        VBox openedRoot = loader.load();
        LoginDialogController controller = loader.getController();
        dialogBase.createDialogStage(tabPane, openedRoot, controller);
        if (controller.isExit()) {
          exit = true;
          break;
        }
      } catch (IOException e) {
        log.error("Не удалось открыть диалог авторизации: ", e);
        uiNotificationService.showError(e.getMessage());
        break;
      }
    }
    if (exit) {
      context.close();
      Platform.exit();
    }
  }

  public void openReturnTab(PaymentReceipt receipt, List<SalesWithProductName> sales) {
    if (returnTab.isDisable()) {
      return;
    }
    if (!tabControllerCache.containsKey(returnTab)) {
      tabInitialize(returnTab);
    }
    Object controller = tabControllerCache.get(returnTab);
    if (controller instanceof TransactionReturnController rc) {
      rc.loadReceiptData(receipt, sales);
    }
    tabPane.getSelectionModel().select(returnTab);
  }

  @EventListener
  public void onLogin(AuthEvents.LoginSuccess ev) {
    this.loggedIn = true;
    this.isAdmin = UserRole.ADMIN.equals(ev.getSource().getRole());
    applyAuthState(true, this.isAdmin);

    try {
      cashShiftService.ensureOpenShift(ev.getSource());
    } catch (Exception ignored) {
    }
    // при желании можно сразу переключить на Продажи:
    // tabPane.getSelectionModel().select(salesTab);
  }

  @EventListener
  public void onLogout(AuthEvents.Logout ev) {
    this.loggedIn = false;
    this.isAdmin = false;
    applyAuthState(false, false);
    tabPane.getSelectionModel().select(shiftChangeTab);

    // Сразу снова показываем диалог логина (пока не войдут)
    Platform.runLater(this::showLoginDialogLoop);
  }

  private void applyAuthState(boolean loggedIn, boolean admin) {
    shiftChangeTab.setDisable(false);
    salesTab.setDisable(!loggedIn);
    returnTab.setDisable(!loggedIn);
    salesHistoryTab.setDisable(!loggedIn);

    // Admin — добавляем/убираем вкладку
    tabPane.getTabs().remove(adminTab);
    if (loggedIn && admin && !tabPane.getTabs().contains(adminTab)) {
      tabPane.getTabs().add(adminTab);
    }
  }

  private void tabInitialize(Tab newTab) {
    if (newTab == null || newTab.isDisable()) {
      return;
    }

    try {
      if (!tabContentCache.containsKey(newTab)) {
        String fxmlPath = getFxmlPath(newTab);
        if (fxmlPath == null || fxmlPath.isBlank()) {
          return;
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        loader.setControllerFactory(context::getBean);

        Node content = loader.load();
        Object controller = loader.getController();

        tabContentCache.put(newTab, content);
        tabControllerCache.put(newTab, controller);
        newTab.setContent(content);
      }
      Object controller = tabControllerCache.get(newTab);
      if (controller instanceof TabController tc) {
        tc.onTabSelected();
      }
    } catch (IOException e) {
      System.out.println("Ошибка загрузки вкладки: " + e.getMessage());
    }
  }

  private String getFxmlPath(Tab tab) {
    if (tab == salesTab) {
      return "/fxml/sales.fxml";
    }
    if (tab == returnTab) {
      return "/fxml/transaction_return_view.fxml";
    }
    if (tab == shiftChangeTab) {
      return "/fxml/shift_change.fxml";
    }
    if (tab == salesHistoryTab) {
      return "/fxml/sale_history.fxml";
    }
    if (tab == adminTab) {
      return "/fxml/admin.fxml";
    }
    return null;
  }
}