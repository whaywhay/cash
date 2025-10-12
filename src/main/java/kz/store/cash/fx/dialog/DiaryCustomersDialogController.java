package kz.store.cash.fx.dialog;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import kz.store.cash.fx.component.FxAsyncRunner;
import kz.store.cash.fx.component.UiNotificationService;
import kz.store.cash.fx.dialog.lib.CancellableDialog;
import kz.store.cash.handler.BusinessException;
import kz.store.cash.model.diarydebt.DiaryCustomer;
import kz.store.cash.model.diarydebt.DiaryTransaction;
import kz.store.cash.model.diarydebt.PageResult;
import kz.store.cash.model.enums.DiaryOperationType;
import kz.store.cash.service.diary.DiaryDebtCustomerApi;
import kz.store.cash.service.diary.DiaryTransactionApi;
import kz.store.cash.util.StringUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DiaryCustomersDialogController implements CancellableDialog {

  @FXML
  public Label totalLabel;
  @FXML
  private BorderPane root;
  @FXML
  private TextField searchField;
  @FXML
  private ChoiceBox<String> orderChoice;
  @FXML
  private TableView<DiaryCustomer> table;
  @FXML
  private TableColumn<DiaryCustomer, Long> idCol;
  @FXML
  private TableColumn<DiaryCustomer, String> nameCol;
  @FXML
  private TableColumn<DiaryCustomer, String> placeCol;
  @FXML
  private TableColumn<DiaryCustomer, String> marketTitleCol;
  @FXML
  private TableColumn<DiaryCustomer, String> totalCol;
  @FXML
  private TableColumn<DiaryCustomer, String> lastCol;
  @FXML
  private Pagination pagination;

  private String currentSearch = null;
  private String currentOrder = "name";
  @Getter
  private boolean writeDebtFlag = false;

  private final FxAsyncRunner fx;
  private final DiaryDebtCustomerApi debtCustomerApi;
  private final DiaryTransactionApi txApi;
  private final UiNotificationService uiNotificationService;
  private double totalToPay;
  @Getter
  private DiaryTransaction diaryTransaction;

  @FXML
  public void initialize() {
    idCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().id()));
    nameCol.setCellValueFactory(
        c -> new ReadOnlyObjectWrapper<>(StringUtils.nullToEmpty(c.getValue().name())));
    placeCol.setCellValueFactory(
        c -> new ReadOnlyObjectWrapper<>(StringUtils.nullToEmpty(c.getValue().place())));
    marketTitleCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
        c.getValue().market() != null && c.getValue().market().title() != null ? c.getValue()
            .market().title() : "")
    );
    totalCol.setCellValueFactory(
        c -> new ReadOnlyObjectWrapper<>(StringUtils.nullToEmpty(c.getValue().total())));
    lastCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
        StringUtils.nullToEmpty(c.getValue().lastTransactionAt())));

    orderChoice.getItems()
        .setAll("name", "-name", "place", "-place", "total", "-total", "last_transaction",
            "-last_transaction");
    orderChoice.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
      currentOrder = b;
      reloadFirstPage();
    });
  }

  public void resetAndReload(double totalToPay) {
    setTotalLabel(totalToPay);
    currentSearch = null;
    currentOrder = "name";
    writeDebtFlag = false;
    diaryTransaction = null;
    if (searchField != null) {
      searchField.clear();
    }
    if (orderChoice != null) {
      orderChoice.getSelectionModel().select("name");
    }
    if (table != null) {
      table.getItems().clear();
    }
    reloadFirstPage();
  }

  private void setTotalLabel(double totalToPay) {
    totalLabel.setText(String.format("%.2f тг", totalToPay));
    this.totalToPay = totalToPay;
  }

  @FXML
  public void onSearch() {
    currentSearch = blankToNull(searchField.getText());
    reloadFirstPage();
  }

  private void reloadFirstPage() {
    pagination.setCurrentPageIndex(0);
    pagination.setPageFactory(this::loadPage);
  }

  private Node loadPage(int pageIndex) {
    int page = pageIndex + 1;
    fx.runWithLoader(root, "Загрузка клиентов...",
        () -> debtCustomerApi.getPage(page, currentSearch, currentOrder),
        this::applyPage);
    return new Label();
  }

  private void applyPage(PageResult<DiaryCustomer> pr) {
    table.getItems().setAll(pr.content());
    pagination.setPageCount(pr.totalPages() > 0 ? pr.totalPages() : 1);
    pagination.setCurrentPageIndex(Math.max(0, pr.page() - 1));
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  @Override
  public void handleClose() {
    Stage stage = (Stage) root.getScene().getWindow();
    stage.close();
  }

  @FXML
  public void onClose() {
    handleClose();
  }

  void checkTotalPay() {
    if (totalToPay <= 0) {
      throw new BusinessException("В долг можете записать только положительную сумму");
    }
  }

  @FXML
  public void onWriteDebt() { // CHANGED — реализация POST
    checkTotalPay();

    DiaryCustomer selected = table.getSelectionModel().getSelectedItem();
    if (selected == null) {
      uiNotificationService.showBusinessError("Выберите из списка для записи в долг");
      return;
    }

    // Сумма для API — целые тены. Округляем до ближайшего целого.
    final int amount = (int) Math.round(totalToPay);
    final long customerId = selected.id();
    diaryTransaction = createDebtTransaction(customerId, amount);
    fx.runWithLoader(root, "Записываем долг...",
        () -> txApi.createSale(diaryTransaction, DiaryOperationType.DEBT_SALE),
        resp -> {
          if (resp != null && resp.getStatusCode() == HttpStatus.CREATED) {
            writeDebtFlag = true;
            log.info("after diaryTransaction: {}", diaryTransaction);
            handleClose(); // закрываем диалог
          } else {
            diaryTransaction = null;
            writeDebtFlag = false;
            uiNotificationService.showError("Не удалось записать долг. Код: " +
                (resp == null ? "N/A" : resp.getStatusCode()));
          }
        });
  }

  private DiaryTransaction createDebtTransaction(long customer, int amount) {
    return new DiaryTransaction(String.valueOf(customer), amount);
  }
}

