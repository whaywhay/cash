package kz.store.cash.fx.controllers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.fx.controllers.lib.TabController;
import kz.store.cash.fx.dialog.ReceiptDetailsController;
import kz.store.cash.fx.dialog.lib.DialogBase;
import kz.store.cash.fx.model.LocationSize;
import kz.store.cash.model.enums.PaymentType;
import kz.store.cash.model.enums.ReceiptStatus;
import kz.store.cash.service.PaymentReceiptService;
import kz.store.cash.util.TableUtils;
import kz.store.cash.util.UtilNumbers;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SalesHistoryController implements TabController {

  @FXML
  public BorderPane rootPane;
  @FXML
  public Button searchButton;
  @FXML
  private TextField searchReceipt;
  @FXML
  private Button cleanSearch;
  @FXML
  private DatePicker dateFilter;
  @FXML
  private Pagination pagination;
  @FXML
  private TableView<PaymentReceipt> salesHistoryTable;
  @FXML
  private TableColumn<PaymentReceipt, Long> idCol;
  @FXML
  private TableColumn<PaymentReceipt, String> dateCol;
  @FXML
  private TableColumn<PaymentReceipt, String> totalCol;
  @FXML
  private TableColumn<PaymentReceipt, String> paymentTypeCol;
  @FXML
  private TableColumn<PaymentReceipt, String> receiptStatusCol;
  @FXML
  private TableColumn<PaymentReceipt, ReceiptStatus> fiscalCol;

  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm:ss");
  private static final int ROWS_PER_PAGE = 25;
  private LocalDate currentFilterDate;
  private final PaymentReceiptService paymentReceiptService;
  private final DialogBase dialogBase;
  private final UtilNumbers utilNumbers;

  @Override
  public void onTabSelected() {
    // Каждый раз при открытии вкладки обновляем данные
    resetFiltersAndRefresh();
  }

  @FXML
  public void initialize() {
    configureColumns();
    // Привязываем ширину колонок через TableUtils
    TableUtils.bindColumnWidths(
        salesHistoryTable,
        new double[]{0.17, 0.15, 0.16, 0.17, 0.16, 0.19},
        // размер для колонок 1 = 100% процент размеру
        idCol, dateCol, totalCol, paymentTypeCol, receiptStatusCol, fiscalCol);
    // Устанавливаем сегодняшнюю дату по умолчанию
    currentFilterDate = LocalDate.now();
    dateFilter.setValue(currentFilterDate);
    // Ограничиваем поле поиска только целыми числами и диапазоном
    utilNumbers.setupLongFilter(searchReceipt, val -> val >= 1 && val <= 999_000_000_000L);
    // События на кнопках и полях
    cleanSearch.setOnAction(e -> resetFiltersAndRefresh());

    searchReceipt.setOnAction(e -> refreshData());
    searchButton.setOnAction(e -> refreshData());

    dateFilter.setOnAction(e -> {
      currentFilterDate = dateFilter.getValue();
      refreshData();
    });
  }

  /**
   * Полный сброс фильтров + загрузка дефолтных данных (используем при открытии вкладки или кнопке
   * Очистить)
   */
  public void resetFiltersAndRefresh() {
    searchReceipt.clear();
    currentFilterDate = LocalDate.now();
    dateFilter.setValue(currentFilterDate);
    refreshDataInternal();
  }

  /**
   * Обновляем данные с текущими значениями фильтров (используется поиском и датой)
   */
  public void refreshData() {
    refreshDataInternal();
  }

  /**
   * Общий метод для установки пагинации и вызова createPage()
   */
  private void refreshDataInternal() {
    pagination.setCurrentPageIndex(0);
    pagination.setPageFactory(this::createPage);
  }

  /**
   * Создание страницы данных
   */
  private Node createPage(int pageIndex) {
    salesHistoryTable.getItems().clear();
    String searchText = searchReceipt.getText();

    Page<PaymentReceipt> page;
    if (searchText != null && !searchText.isBlank()) {
      // Точный поиск по ID чека (без учета даты)
      page = paymentReceiptService.findById(Long.valueOf(searchText),
          PageRequest.of(pageIndex, ROWS_PER_PAGE));
    } else {
      // Фильтрация по дате (последние продажи за выбранный день)
      LocalDateTime startOfDay = currentFilterDate.atStartOfDay();
      LocalDateTime endOfDay = currentFilterDate.plusDays(1).atStartOfDay();

      page = paymentReceiptService.findByCreatedDate(startOfDay, endOfDay,
          PageRequest.of(pageIndex, ROWS_PER_PAGE));
    }
    salesHistoryTable.getItems().setAll(page.getContent());
    pagination.setPageCount(page.getTotalPages() > 0 ? page.getTotalPages() : 1);
    return new StackPane();
  }

  /**
   * Настройка колонок таблицы
   */
  private void configureColumns() {
    idCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));

    dateCol.setCellValueFactory(data ->
        new ReadOnlyObjectWrapper<>(formatter.format(data.getValue().getCreated())));

    totalCol.setCellValueFactory(data ->
        new ReadOnlyObjectWrapper<>(String.format("%s тг", data.getValue().getTotal())));

    paymentTypeCol.setCellValueFactory(data -> {
      PaymentType type = data.getValue().getPaymentType();
      return new ReadOnlyObjectWrapper<>(type != null ? type.getDisplayName() : "");
    });

    receiptStatusCol.setCellValueFactory(data -> {
      ReceiptStatus type = data.getValue().getReceiptStatus();
      return new ReadOnlyObjectWrapper<>(type != null ? type.getDisplayName() : "");
    });

    fiscalCol.setCellFactory(col -> new TableCell<>() {
      private final Label link = new Label("Открыть чек");

      {
        link.setStyle("-fx-text-fill: teal; -fx-underline: true; -fx-cursor: hand;");
        link.setOnMouseClicked(e -> {
          PaymentReceipt receipt = getTableView().getItems().get(getIndex());
          if (receipt != null) {
            showReceptionDialogDetails(receipt);
          }
        });
      }

      @Override
      protected void updateItem(ReceiptStatus status, boolean empty) {
        super.updateItem(status, empty);
        if (empty || status == null) {
          setGraphic(null);
        } else {
          setGraphic(link);
        }
      }
    });
    fiscalCol.setCellValueFactory(
        data -> new ReadOnlyObjectWrapper<>(data.getValue().getReceiptStatus()));
  }

  /**
   * Открытие деталей чека
   */
  public void showReceptionDialogDetails(PaymentReceipt reception) {
    try {
      var loader = dialogBase.loadFXML("/fxml/receipt_details.fxml");
      BorderPane openedRoot = loader.load();
      ReceiptDetailsController controller = loader.getController();
      LocationSize locationSize = setReceptionDialogLocation();
      controller.sendReceipt(reception);
      dialogBase.createWithLocationDialogStage(rootPane, openedRoot, controller, locationSize);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private LocationSize setReceptionDialogLocation() {
    double width =
        fiscalCol.getWidth() + receiptStatusCol.getWidth() + paymentTypeCol.getWidth();
    double x = salesHistoryTable.getScene().getWindow().getX()
        + salesHistoryTable.getScene().getWindow().getWidth() - width;
    double y = salesHistoryTable.getScene().getWindow().getY();
    double height = salesHistoryTable.getScene().getWindow().getHeight();
    return new LocationSize(x, y, width, height);
  }
}