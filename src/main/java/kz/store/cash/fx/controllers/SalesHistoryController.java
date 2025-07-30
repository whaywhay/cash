package kz.store.cash.fx.controllers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
import kz.store.cash.entity.PaymentReceipt;
import kz.store.cash.fx.dialog.ReceiptDetailsController;
import kz.store.cash.fx.dialog.lib.DialogBase;
import kz.store.cash.fx.model.LocationSize;
import kz.store.cash.model.enums.PaymentType;
import kz.store.cash.model.enums.ReceiptStatus;
import kz.store.cash.repository.PaymentReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SalesHistoryController {

  @FXML
  public BorderPane rootPane;
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
  private TableColumn<PaymentReceipt, ReceiptStatus> fiscalCol;

  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
  private static final int ROWS_PER_PAGE = 35;
  private LocalDate currentFilterDate;
  private final PaymentReceiptRepository receiptRepository;
  private final DialogBase dialogBase;

  @FXML
  public void initialize() {
    configureColumns();
    bindColumnWidths();
    currentFilterDate = LocalDate.now();
    dateFilter.setValue(currentFilterDate);
    pagination.setMaxPageIndicatorCount(3);
    pagination.setPageFactory(this::createPage);
    cleanSearch.setOnAction(e -> {
      searchReceipt.clear();
      dateFilter.setValue(LocalDate.now());
      currentFilterDate = LocalDate.now();
      pagination.setCurrentPageIndex(0);
      pagination.setPageFactory(this::createPage);
    });
    searchReceipt.setOnAction(e -> {
      pagination.setCurrentPageIndex(0);
      pagination.setPageFactory(this::createPage);
    });
    dateFilter.setOnAction(e -> {
      currentFilterDate = dateFilter.getValue();
      pagination.setCurrentPageIndex(0);
      pagination.setPageFactory(this::createPage);
    });
  }

  private void bindColumnWidths() {
    salesHistoryTable.widthProperty().addListener((obs, oldVal, newVal) -> {
      double width = newVal.doubleValue();
      idCol.setPrefWidth(width * 0.10);
      dateCol.setPrefWidth(width * 0.15);
      totalCol.setPrefWidth(width * 0.15);
      paymentTypeCol.setPrefWidth(width * 0.20);
      fiscalCol.setPrefWidth(width * 0.40);
    });
  }

  private Node createPage(int pageIndex) {
    LocalDateTime startOfDay = currentFilterDate.atStartOfDay();
    LocalDateTime endOfDay = currentFilterDate.plusDays(1).atStartOfDay();

    Page<PaymentReceipt> page = receiptRepository.findByCreatedDate(
        startOfDay,
        endOfDay,
        PageRequest.of(pageIndex, ROWS_PER_PAGE)
    );
    List<PaymentReceipt> filtered = page.getContent();
    String searchText = searchReceipt.getText();
    if (searchText != null && !searchText.isBlank()) {
      filtered = filtered.stream()
          .filter(r -> String.valueOf(r.getId()).contains(searchText))
          .toList();
    }
    salesHistoryTable.getItems().setAll(filtered);
    pagination.setPageCount(page.getTotalPages() > 0 ? page.getTotalPages() : 1);
    return new StackPane();
  }

  private void configureColumns() {
    idCol.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));

    dateCol.setCellValueFactory(
        data -> new ReadOnlyObjectWrapper<>(formatter.format(data.getValue().getCreated())));

    totalCol.setCellValueFactory(data ->
        new ReadOnlyObjectWrapper<>(String.format("%s тг", data.getValue().getTotal()))
    );

    paymentTypeCol.setCellValueFactory(data -> {
      PaymentType type = data.getValue().getPaymentType();
      return new ReadOnlyObjectWrapper<>(type != null ? type.getDisplayName() : "");
    });

    fiscalCol.setCellFactory(col -> new TableCell<>() {
      private final Label link = new Label("Отрыть чек");

      {
        link.setStyle("-fx-text-fill: teal; -fx-underline: true; -fx-cursor: hand;");
        link.setOnMouseClicked(e -> {
          PaymentReceipt receipt = getTableView().getItems().get(getIndex());
          if (receipt != null) {
            Long id = receipt.getId();
            System.out.println("Ссылка нажата, ID: " + id);
            showReceptionDetails(receipt);
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

  public void showReceptionDetails(PaymentReceipt reception) {
    try {
      var loader = dialogBase.loadFXML("/fxml/receipt_details.fxml");
      BorderPane openedRoot = loader.load();
      ReceiptDetailsController controller = loader.getController();
      LocationSize locationSize = setLocation();
      controller.sendReceipt(reception);
      dialogBase.createWithLocationDialogStage(rootPane, openedRoot, controller, locationSize);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private LocationSize setLocation() {
    double x = salesHistoryTable.getScene().getWindow().getX()
        + salesHistoryTable.getScene().getWindow().getWidth() - 320;
    double y = salesHistoryTable.getScene().getWindow().getY();
    double width = 320;
    double height = salesHistoryTable.getScene().getWindow().getHeight();
    return new LocationSize(x, y, width, height);
  }
}
