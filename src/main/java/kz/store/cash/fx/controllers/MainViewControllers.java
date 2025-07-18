package kz.store.cash.fx.controllers;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MainViewControllers {

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

  private Node salesContent;
  private Node returnContent;
  private Node shiftContent;
  private Node saleHistoryContent;

  @FXML
  public void initialize() {
    tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab)
        -> tabInitialize(newTab));
    tabInitialize(tabPane.getSelectionModel().getSelectedItem());


  }

  private void tabInitialize(Tab newTab) {
    try {
      if (newTab == salesTab && salesContent == null) {
        salesContent = loadFXML("/fxml/sales.fxml");
        salesTab.setContent(salesContent);
      } else if (newTab == returnTab && returnContent == null) {
        returnContent = loadFXML("/fxml/transaction_return_view.fxml");
        returnTab.setContent(returnContent);
      } else if (newTab == shiftChangeTab && shiftContent == null) {
        shiftContent = loadFXML("/fxml/shift_change.fxml");
        shiftChangeTab.setContent(shiftContent);
      } else if (newTab == salesHistoryTab && saleHistoryContent == null) {
        saleHistoryContent = loadFXML("/fxml/sale_history.fxml");
        salesHistoryTab.setContent(saleHistoryContent);
      }
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  private Node loadFXML(String path) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
    loader.setControllerFactory(context::getBean);
    return loader.load();
  }
}
