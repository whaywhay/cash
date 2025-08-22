package kz.store.cash.fx.controllers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.fx.controllers.lib.TabController;
import kz.store.cash.fx.model.SalesWithProductName;
import kz.store.cash.model.enums.UserRole;
import kz.store.cash.security.AuthEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MainViewController {

  private final ApplicationContext context;

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

  // текущее состояние авторизации (без завязки на конкретный класс пользователя)
  private boolean loggedIn = false;
  private boolean isAdmin = false;

  @FXML
  public void initialize() {
    // изначально считаем, что пользователь не залогинен
    applyAuthState(false, false);

    // слушаем смену вкладок — инициализируем контент только когда вкладка активируется
    tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
      // если по какой-то причине оказались на недоступной вкладке — вернёмся на “Смена”
      if (newTab != null && newTab.isDisable()) {
        tabPane.getSelectionModel().select(shiftChangeTab);
        return;
      }
      tabInitialize(newTab);
    });

    // при старте явно активируем "Смена"
    tabPane.getSelectionModel().select(shiftChangeTab);
    tabInitialize(shiftChangeTab);
  }

  /**
   * Открыть вкладку "Возврат" и подать туда данные.
   */
  public void openReturnTab(PaymentReceipt receipt, List<SalesWithProductName> sales) {
    // если нет доступа к вкладке — игнорируем
    if (returnTab.isDisable()) {
      return;
    }

    if (!tabControllerCache.containsKey(returnTab)) {
      tabInitialize(returnTab);
    }
    Object controller = tabControllerCache.get(returnTab);
    if (controller instanceof TransactionReturnController returnController) {
      returnController.loadReceiptData(receipt, sales);
    }
    tabPane.getSelectionModel().select(returnTab);
  }

  /**
   * Реагируем на успешный вход.
   */
  @EventListener
  public void onLogin(AuthEvents.LoginSuccess ev) {
    this.loggedIn = true;

    // Сравниваем админ или нет по роли
    UserRole role = ev.getSource().getRole();
    this.isAdmin = UserRole.ADMIN.equals(role);

    applyAuthState(true, this.isAdmin);

    // по UX — сразу перекидываем на "Продажи";
    tabPane.getSelectionModel().select(salesTab);
  }

  /**
   * Реагируем на выход.
   */
  @EventListener
  public void onLogout() {
    this.loggedIn = false;
    this.isAdmin = false;
    applyAuthState(loggedIn, isAdmin);

    // возвращаем на "Смена"
    tabPane.getSelectionModel().select(shiftChangeTab);
  }

  /**
   * Включение/отключение вкладок в зависимости от роли.
   */
  private void applyAuthState(boolean loggedIn, boolean admin) {
    // “Смена” — всегда доступна
    shiftChangeTab.setDisable(false);

    // Все рабочие вкладки — только после логина
    salesTab.setDisable(!loggedIn);
    returnTab.setDisable(!loggedIn);
    salesHistoryTab.setDisable(!loggedIn);

    tabPane.getTabs().remove(adminTab);

    if (loggedIn && admin) {
      tabPane.getTabs().add(adminTab);
    }
  }

  private void tabInitialize(Tab newTab) {
    if (newTab == null) {
      return;
    }
    if (newTab.isDisable()) {
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
      if (controller instanceof TabController tabController) {
        tabController.onTabSelected();
      }
    } catch (IOException e) {
      System.out.println("Ошибка загрузки вкладки: " + e.getMessage());
    }
  }

  /**
   * Маршрутизация FXML-файлов для вкладок.
   */
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