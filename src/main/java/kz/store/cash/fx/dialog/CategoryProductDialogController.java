package kz.store.cash.fx.dialog;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import kz.store.cash.fx.component.UiNotificationService;
import kz.store.cash.fx.dialog.lib.CancellableDialog;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.mapper.ProductMapper;
import kz.store.cash.model.entity.Category;
import kz.store.cash.model.entity.Product;
import kz.store.cash.model.enums.PriceMode;
import kz.store.cash.service.CategoryService;
import kz.store.cash.service.ProductService;
import kz.store.cash.service.QuickProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryProductDialogController implements CancellableDialog {

  @FXML
  public VBox root;
  @FXML
  private FlowPane breadcrumbs;
  @FXML
  private ListView<Row> itemsList;
  @FXML
  private Button backBtn;
  @FXML
  private Button chooseBtn;
  @FXML
  private Button addQuickBtn;
  @FXML
  private Button closeBtn;

  private final CategoryService categoryService;
  private final ProductService productService;
  private final ProductMapper productMapper;
  private final QuickProductService quickProductService;
  private final UiNotificationService uiNotificationService;

  private record Crumb(String code, String name) {

  }

  private static final Crumb ROOT = new Crumb("", "Товары");

  private final Deque<Crumb> path = new ArrayDeque<>();
  private String realRootCode;

  private PriceMode priceMode = PriceMode.ORIGINAL;
  private Consumer<ProductItem> onProductChosen;

  public void init(PriceMode priceMode, Consumer<ProductItem> onProductChosen) {
    this.priceMode = (priceMode == null) ? PriceMode.ORIGINAL : priceMode;
    this.onProductChosen = onProductChosen;

    setupListView();
    setupButtons();

    breadcrumbs.prefWrapLengthProperty().bind(root.widthProperty().subtract(24));

    path.clear();
    path.push(ROOT);
    navigateTo(ROOT);
  }

  private void setupListView() {
    itemsList.setCellFactory(lv -> new ListCell<>() {
      @Override
      protected void updateItem(Row item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setGraphic(null);
          return;
        }
        Label text = new Label(item.title());
        FontIcon icon = new FontIcon(item.type() == RowType.CATEGORY ? "fas-folder" : "fas-box");
        HBox box = new HBox(8, icon, text);
        setGraphic(box);
      }
    });

    // 1 клик — зайти в категорию; 2 клика по товару — выбрать
    itemsList.setOnMouseClicked(e -> {
      Row row = itemsList.getSelectionModel().getSelectedItem();
      if (row == null) {
        return;
      }
      if (row.type() == RowType.CATEGORY && e.getClickCount() == 1) {
        var c = row.category();
        pushAndNavigate(new Crumb(nvl(c.getCategoryCode()), nvl(c.getCategoryName())));
      } else if (row.type() == RowType.PRODUCT && e.getClickCount() == 2) {
        chooseProduct(row.product());
      }
    });

    itemsList.setOnKeyPressed(e -> {
      if (e.getCode() != KeyCode.ENTER) {
        return;
      }
      Row row = itemsList.getSelectionModel().getSelectedItem();
      if (row == null) {
        return;
      }
      if (row.type() == RowType.CATEGORY) {
        var c = row.category();
        pushAndNavigate(new Crumb(nvl(c.getCategoryCode()), nvl(c.getCategoryName())));
      } else {
        chooseProduct(row.product());
      }
    });

    itemsList.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
      boolean isProduct = n != null && n.type() == RowType.PRODUCT;
      chooseBtn.setDisable(!isProduct);
      addQuickBtn.setDisable(!isProduct);
      if (isProduct) {
        updateQuickButtonText(n.product());
      }
    });
  }

  private void setupButtons() {
    backBtn.setOnAction(e -> onBack());
    closeBtn.setOnAction(e -> onClose());

    chooseBtn.setOnAction(e -> {
      Row row = itemsList.getSelectionModel().getSelectedItem();
      if (row != null && row.type() == RowType.PRODUCT) {
        chooseProduct(row.product());
      }
    });

    addQuickBtn.setOnAction(e -> {
      Row row = itemsList.getSelectionModel().getSelectedItem();
      if (row == null || row.type() != RowType.PRODUCT) {
        return;
      }
      var bc = row.product().getBarcode();
      boolean nowQuick = quickProductService.toggle(bc);
      updateQuickButtonText(row.product());

      uiNotificationService.showInfo("В \"БЫСТРЫЕ ТОВАР\" " + (nowQuick ? "ДОБАВЛЕН" : "УДАЛЕН"));
    });

    chooseBtn.setDisable(true);
    addQuickBtn.setDisable(true);
  }

  private void onBack() {
    if (path.size() <= 1) {
      return;
    }
    path.pop();
    navigateTo(path.peek());
  }

  private void onClose() {
    handleClose();
  }

  private void pushAndNavigate(Crumb crumb) {
    path.push(crumb);
    navigateTo(crumb);
  }

  private void navigateTo(Crumb current) {
    rebuildBreadcrumbs();

    final List<Category> categories;
    final List<Product> products;

    if (isRoot(current)) {
      if (realRootCode == null) {
        realRootCode = resolveRealRootCode();
      }
      categories = (realRootCode == null) ? List.of()
          : categoryService.findByParentCategoryId(realRootCode);
      products = List.of(); // в визуальном корне товары не показываем
    } else {
      categories = categoryService.findByParentCategoryId(current.code());
      products = productService.findByCategoryRef_CategoryCode(current.code());
    }

    List<Row> rows = categories.stream().map(Row::fromCategory)
        .collect(Collectors.toCollection(ArrayList::new));
    rows.addAll(products.stream().map(Row::fromProduct).toList());
    itemsList.getItems().setAll(rows);

    backBtn.setDisable(path.size() <= 1);
  }

  private String resolveRealRootCode() {
    List<Category> roots = categoryService.findRoots();
    if (roots.isEmpty()) {
      return null;
    }

    Optional<Category> named = roots.stream()
        .filter(c -> "товары".equalsIgnoreCase(nvl(c.getCategoryName())))
        .findFirst();

    Category rootCat = named.orElse(roots.getFirst());
    return nvl(rootCat.getCategoryCode());
  }

  private void rebuildBreadcrumbs() {
    breadcrumbs.getChildren().clear();

    var trail = new ArrayList<>(path);
    java.util.Collections.reverse(trail);

    for (int i = 0; i < trail.size(); i++) {
      Crumb c = trail.get(i);

      Hyperlink link = new Hyperlink(c.name());
      link.setTextOverrun(OverrunStyle.ELLIPSIS);
      link.setTooltip(new Tooltip(c.name()));
      link.setOnAction(e -> {
        while (!path.isEmpty() && !Objects.equals(path.peek().code(), c.code())) {
          path.pop();
        }
        navigateTo(path.peek());
      });

      breadcrumbs.getChildren().add(link);
      if (i < trail.size() - 1) {
        breadcrumbs.getChildren().add(new Label(" > "));
      }
    }
  }

  private void updateQuickButtonText(Product p) {
    boolean quick = quickProductService.isQuick(p.getBarcode());
    addQuickBtn.setText(quick ? "Убрать из быстрых" : "В быстрые товары");
  }

  private void chooseProduct(Product product) {
    ProductItem item = productMapper.toProductItem(product);
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

  private static boolean isRoot(Crumb c) {
    return c != null && Objects.equals(c.code(), ROOT.code());
  }

  private static String nvl(String s) {
    return s == null ? "" : s;
  }

  @Override
  public void handleClose() {
    Stage stage = (Stage) root.getScene().getWindow();
    stage.close();
  }

  private enum RowType {CATEGORY, PRODUCT}

  private record Row(RowType type, String title, Category category, Product product) {

    static Row fromCategory(Category c) {
      return new Row(RowType.CATEGORY, c.getCategoryName(), c, null);
    }

    static Row fromProduct(Product p) {
      return new Row(RowType.PRODUCT, p.getProductName(), null, p);
    }
  }
}