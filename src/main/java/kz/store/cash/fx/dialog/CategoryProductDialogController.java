package kz.store.cash.fx.dialog;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import kz.store.cash.fx.component.FxAsyncRunner;
import kz.store.cash.fx.dialog.lib.CancellableDialog;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.mapper.ProductMapper;
import kz.store.cash.model.entity.Category;
import kz.store.cash.model.entity.Product;
import kz.store.cash.model.enums.PriceMode;
import kz.store.cash.repository.CategoryRepository;
import kz.store.cash.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryProductDialogController implements CancellableDialog {

  @FXML
  public VBox categoryProductBrowserPane;
  @FXML
  private ListView<Row> itemsList;
  @FXML
  private TextField searchField;
  @FXML
  private Button backBtn;
  @FXML
  private HBox breadcrumbs;

  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository;
  private final ProductMapper productMapper;
  private final FxAsyncRunner fxAsyncRunner;

  /* ---------- Навигация и данные ---------- */

  private record Crumb(String code, String name) {

  }

  private static final Crumb ROOT_CRUMB = new Crumb("", "Корень");

  private final Deque<Crumb> path = new ArrayDeque<>();

  private final ObservableList<Row> masterRows = FXCollections.observableArrayList();
  private final FilteredList<Row> filteredRows = new FilteredList<>(masterRows, r -> true);

  private PriceMode priceMode = PriceMode.ORIGINAL;
  private Consumer<ProductItem> onProductChosen;

  public void init(PriceMode priceMode, Consumer<ProductItem> onProductChosen) {
    this.priceMode = (priceMode == null) ? PriceMode.ORIGINAL : priceMode;
    this.onProductChosen = onProductChosen;

    setupListCell();
    setupSearch();

    // стартуем с корня
    path.clear();
    path.push(ROOT_CRUMB);
    itemsList.setItems(filteredRows);
    navigateToCrumb(path.peek());
  }

  /* ---------- UI сетап ---------- */

  private void setupListCell() {
    itemsList.setCellFactory(lv -> new ListCell<>() {
      @Override
      protected void updateItem(Row item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
          setGraphic(null);
          return;
        }
        Label text = new Label(item.title());
        FontIcon icon = new FontIcon(item.type() == RowType.CATEGORY ? "fas-folder" : "fas-box");
        HBox box = new HBox(8, icon, text);
        setGraphic(box);
      }
    });

    itemsList.setOnMouseClicked(e -> {
      if (e.getClickCount() == 2) {
        openSelected();
      }
    });
    itemsList.setOnKeyPressed(e -> {
      if (e.getCode() == KeyCode.ENTER) {
        openSelected();
      }
    });

    // backBtn включается, когда в стеке больше одного элемента (не только корень)
    backBtn.disableProperty().bind(javafx.beans.binding.Bindings.createBooleanBinding(
        () -> path.size() <= 1, // только "Корень" в стеке
        masterRows
        // зависимость фиктивная, чтобы триггерилось; можно убрать биндинг и управлять вручную
    ));
  }

  private void setupSearch() {
    searchField.textProperty().addListener((obs, oldV, newV) -> {
      String q = (newV == null) ? "" : newV.trim().toLowerCase();
      if (q.isEmpty()) {
        filteredRows.setPredicate(r -> true);
      } else {
        filteredRows.setPredicate(r -> r.title().toLowerCase().contains(q));
      }
    });
  }

  /* ---------- Действия ---------- */

  private void openSelected() {
    Row row = itemsList.getSelectionModel().getSelectedItem();
    if (row == null) {
      return;
    }

    switch (row.type()) {
      case CATEGORY -> {
        var c = row.category();
        var crumb = new Crumb(nvl(c.getCategoryCode()), nvl(c.getCategoryName()));
        path.push(crumb);
        navigateToCrumb(crumb);
      }
      case PRODUCT -> chooseProduct(row.product());
    }
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

  @FXML
  private void onBack() {
    if (path.size() <= 1) {
      return; // уже корень
    }
    path.pop();
    navigateToCrumb(path.peek());
  }

  @FXML
  private void onClose() {
    handleClose();
  }

  /* ---------- Навигация и отрисовка ---------- */

  private void navigateToCrumb(Crumb current) {
    fxAsyncRunner.runWithLoader(itemsList, "Загрузка...", () -> {
      // категории-папки в текущей корневой/категории
      List<Category> cats = current.code().isEmpty()
          ? categoryRepository.findRoots()
          : categoryRepository.findByParentCategoryId(current.code());

      // товары в текущей категории (только для не-корня)
      List<Product> prods = current.code().isEmpty()
          ? List.of()
          : productRepository.findByCategoryRef_CategoryCode(current.code());

      return new Payload(cats, prods);
    }, this::renderPayload);
  }

  private void renderPayload(Payload data) {
    rebuildBreadcrumbs();

    List<Row> rows = data.categories().stream()
        .map(Row::fromCategory)
        .collect(Collectors.toCollection(ArrayList::new));

    rows.addAll(
        data.products().stream()
            .map(Row::fromProduct)
            .toList()
    );

    masterRows.setAll(rows);
  }

  private void rebuildBreadcrumbs() {
    breadcrumbs.getChildren().clear();

    // рисуем крошки от корня к текущей
    List<Crumb> trail = new ArrayList<>(path);
    java.util.Collections.reverse(trail);

    for (int i = 0; i < trail.size(); i++) {
      Crumb c = trail.get(i);
      Button b = new Button(c.name());
      b.getStyleClass().add("link-button");
      b.setOnAction(e -> {
        // "прыжок" на выбранную крошку: поп пока сверху не будет она
        while (!path.isEmpty() && !Objects.equals(path.peek().code(), c.code())) {
          path.pop();
        }
        navigateToCrumb(path.peek());
      });
      breadcrumbs.getChildren().add(b);
      if (i < trail.size() - 1) {
        breadcrumbs.getChildren().add(new Label(">"));
      }
    }
  }

  private static String nvl(String s) {
    return s == null ? "" : s;
  }

  @Override
  public void handleClose() {
    Stage stage = (Stage) categoryProductBrowserPane.getScene().getWindow();
    stage.close();
  }

  /* ---------- Модели строк ---------- */

  private enum RowType {CATEGORY, PRODUCT}

  private record Row(RowType type, String title, Category category, Product product) {

    static Row fromCategory(Category c) {
      return new Row(RowType.CATEGORY, c.getCategoryName(), c, null);
    }

    static Row fromProduct(Product p) {
      return new Row(RowType.PRODUCT, p.getProductName(), null, p);
    }
  }

  private record Payload(List<Category> categories, List<Product> products) {

  }
}