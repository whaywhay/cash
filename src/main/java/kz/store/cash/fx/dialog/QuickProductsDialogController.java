package kz.store.cash.fx.dialog;

import java.util.List;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import kz.store.cash.fx.dialog.lib.CancellableDialog;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.mapper.ProductMapper;
import kz.store.cash.model.entity.Product;
import kz.store.cash.model.enums.PriceMode;
import kz.store.cash.service.QuickProductService;
import lombok.RequiredArgsConstructor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuickProductsDialogController implements CancellableDialog {

  @FXML
  public VBox root;
  @FXML
  private ListView<Product> itemsList;
  @FXML
  private Button removeBtn;
  @FXML
  private Button chooseBtn;
  @FXML
  private Button closeBtn;
  // @FXML private Button upBtn;   // на будущее
  // @FXML private Button downBtn; // на будущее

  private final QuickProductService quickService;
  private final ProductMapper productMapper;

  private PriceMode priceMode = PriceMode.ORIGINAL;
  private Consumer<ProductItem> onProductChosen;

  public void init(PriceMode priceMode, Consumer<ProductItem> onProductChosen) {
    this.priceMode = (priceMode == null) ? PriceMode.ORIGINAL : priceMode;
    this.onProductChosen = onProductChosen;
    setupList();
    setupButtons();
    loadData();
  }

  private void setupList() {
    itemsList.setCellFactory(lv -> new ListCell<>() {
      @Override
      protected void updateItem(Product p, boolean empty) {
        super.updateItem(p, empty);
        if (empty || p == null) {
          setGraphic(null);
          return;
        }
        var name = new Label(p.getProductName());
        var sub = new Label("ШК: " + p.getBarcode());
        sub.getStyleClass().add("subtle");
        var box = new VBox(name, sub);
        var icon = new FontIcon("fas-bolt");
        icon.getStyleClass().add("icon-accent");
        var row = new HBox(8, icon, box);
        setGraphic(row);
      }
    });

    // двойной клик — выбрать
    itemsList.setOnMouseClicked(e -> {
      Product p = itemsList.getSelectionModel().getSelectedItem();
      if (p != null && e.getClickCount() == 2) {
        choose(p);
      }
    });

    itemsList.setOnKeyPressed(e -> {
      if (e.getCode() == KeyCode.ENTER) {
        Product p = itemsList.getSelectionModel().getSelectedItem();
        if (p != null) {
          choose(p);
        }
      }
    });

    itemsList.getSelectionModel().selectedItemProperty().addListener((o, oldV, newV) -> {
      boolean has = newV != null;
      chooseBtn.setDisable(!has);
      removeBtn.setDisable(!has);
      // upBtn.setDisable(!has);
      // downBtn.setDisable(!has);
    });
  }

  private void setupButtons() {
    closeBtn.setOnAction(e -> handleClose());
    chooseBtn.setOnAction(e -> {
      var p = itemsList.getSelectionModel().getSelectedItem();
      if (p != null) {
        choose(p);
      }
    });
    removeBtn.setOnAction(e -> {
      var p = itemsList.getSelectionModel().getSelectedItem();
      if (p != null) {
        quickService.toggle(p.getBarcode()); // удалим
        loadData();
      }
    });
    // upBtn.setOnAction(e -> {/* TODO сортировка */});
    // downBtn.setOnAction(e -> {/* TODO сортировка */});

    chooseBtn.setDisable(true);
    removeBtn.setDisable(true);
  }

  private void loadData() {
    List<Product> data = quickService.listProducts();
    itemsList.getItems().setAll(data);
  }

  private void choose(Product p) {
    ProductItem item = productMapper.toProductItem(p);
    if (priceMode == PriceMode.WHOLESALE) {
      item.setToWholesalePrice();
    } else {
      item.setToOriginalPrice();
    }
    if (onProductChosen != null) {
      onProductChosen.accept(item);
    }
    handleClose();
  }

  @Override
  public void handleClose() {
    Stage stage = (Stage) root.getScene().getWindow();
    stage.close();
  }
}
