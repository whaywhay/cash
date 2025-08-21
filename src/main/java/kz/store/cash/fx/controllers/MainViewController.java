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
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
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

  // Кэшируем контент и контроллеры для каждой вкладки
  private final Map<Tab, Node> tabContentCache = new HashMap<>();
  private final Map<Tab, Object> tabControllerCache = new HashMap<>();


  @FXML
  public void initialize() {
    tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab)
        -> tabInitialize(newTab));

    // Инициализация первой вкладки
    tabInitialize(tabPane.getSelectionModel().getSelectedItem());
  }

  public void openReturnTab(PaymentReceipt receipt, List<SalesWithProductName> sales) {
    // Загружаем вкладку Возврат, если она не инициализирована
    if (!tabControllerCache.containsKey(returnTab)) {
      tabInitialize(returnTab);
    }
    Object controller = tabControllerCache.get(returnTab);
    if (controller instanceof TransactionReturnController returnController) {
      returnController.loadReceiptData(receipt, sales);
    }
    tabPane.getSelectionModel().select(returnTab);
  }


  private void tabInitialize(Tab newTab) {
    if (newTab == null) {
      return;
    }

    try {
      // Загружаем FXML и контроллер, если вкладка ещё не инициализирована
      if (!tabContentCache.containsKey(newTab)) {
        String fxmlPath = getFxmlPath(newTab);
        if (fxmlPath == null) {
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
      // Вызываем onTabSelected() у контроллера (если он реализует интерфейс)
      Object controller = tabControllerCache.get(newTab);
      if (controller instanceof TabController tabController) {
        tabController.onTabSelected();
      }
    } catch (IOException e) {
      System.out.println("Ошибка загрузки вкладки: " + e.getMessage());
    }
  }

  /**
   * Возвращает путь к FXML для конкретной вкладки
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
    return null;
  }
}
