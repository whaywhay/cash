package kz.store.cash.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javafx.collections.FXCollections;
import kz.store.cash.SqliteSchemaTestApplication;
import kz.store.cash.fx.component.ReceiptPrintService;
import kz.store.cash.fx.model.PaymentSumDetails;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.handler.BusinessException;
import kz.store.cash.model.CategoryDto;
import kz.store.cash.model.ProductDto;
import kz.store.cash.model.diarydebt.DiaryTransaction;
import kz.store.cash.model.UserDto;
import kz.store.cash.model.entity.AppSetting;
import kz.store.cash.model.entity.CashShift;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.model.entity.Product;
import kz.store.cash.model.entity.User;
import kz.store.cash.model.enums.CashMovementType;
import kz.store.cash.model.enums.CashShiftStatus;
import kz.store.cash.model.enums.PaymentType;
import kz.store.cash.model.enums.UserRole;
import kz.store.cash.model.enums.ReceiptStatus;
import kz.store.cash.repository.CashShiftRepository;
import kz.store.cash.repository.CategoryRepository;
import kz.store.cash.repository.PaymentReceiptRepository;
import kz.store.cash.repository.ProductRepository;
import kz.store.cash.repository.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Денежные потоки кассы на настоящей файловой SQLite (Flyway-схема, Hibernate, MapStruct-мапперы,
 * транзакции Spring): продажа, возврат, долг, отложенные чеки, закрытие смены и синхронизация 1С.
 * Мокито-тесты сервисов проверяют только логику вызовов — здесь проверяется то, что реально лежит
 * в БД: суммы SQL-агрегатов, связи чеков и продаж, откат транзакций.
 * Печать смены ({@link ReceiptPrintService}) подменена, остальное — боевое.
 */
@SpringBootTest(classes = SqliteSchemaTestApplication.class)
class CashFlowSqliteIntegrationTest {

  private static final String UNIVERSAL_BARCODE = "universal-product-200001";
  private static Path dbFile;

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) throws IOException {
    dbFile = Files.createTempFile("cash-flow-test-", ".db");
    Files.deleteIfExists(dbFile);
    registry.add("spring.datasource.url",
        () -> "jdbc:sqlite:" + dbFile.toAbsolutePath() + "?foreign_keys=on");
  }

  @MockitoBean ReceiptPrintService receiptPrintService;

  @Autowired JdbcTemplate jdbc;
  @Autowired CashShiftService shifts;
  @Autowired PaymentReceiptService receipts;
  @Autowired SalesService sales;
  @Autowired OneCSyncWriter writer;
  @Autowired UserRepository users;
  @Autowired ProductRepository products;
  @Autowired CategoryRepository categories;
  @Autowired CashShiftRepository shiftRepository;
  @Autowired PaymentReceiptRepository receiptRepository;
  @Autowired UserService userService;
  @Autowired AppSettingService appSettings;
  @Autowired PasswordEncoder passwordEncoder;

  User admin;

  @BeforeEach
  @SuppressWarnings("SqlWithoutWhere") // очистка таблиц между тестами намеренно без where
  void cleanDatabase() {
    jdbc.update("delete from sales");
    jdbc.update("delete from payment_receipt");
    jdbc.update("delete from cash_movement");
    jdbc.update("delete from cash_shift");
    jdbc.update("delete from app_settings");
    jdbc.update("delete from user where username <> 'admin'");
    jdbc.update("delete from product where barcode <> ?", UNIVERSAL_BARCODE);
    jdbc.update("delete from category");
    admin = users.findAll().getFirst();
  }

  // ---------- смена ----------

  @Test void onlyOneShiftCanBeOpenAndNextOneStartsWithCashLeftInDrawer() {
    CashShift first = shifts.openShift(admin);
    assertThat(first.getStatus()).isEqualTo(CashShiftStatus.OPENED);
    assertThat(first.getCashDuringOpening()).isEqualByComparingTo("0");

    assertThatThrownBy(() -> shifts.openShift(admin)).isInstanceOf(IllegalStateException.class);
    shifts.ensureOpenShift(admin);
    assertThat(count("cash_shift")).isEqualTo(1);

    shifts.closeShift(first.getId(), new BigDecimal("500.00"), "конец дня", admin);

    CashShift second = shifts.openShift(admin);
    assertThat(second.getCashDuringOpening()).isEqualByComparingTo("500.00");
    assertThat(second.getId()).isNotEqualTo(first.getId());
  }

  @Test void databaseItselfRejectsSecondOpenShift() {
    shifts.openShift(admin);

    assertThatThrownBy(() -> shiftRepository.save(newOpenShift()))
        .isInstanceOf(JpaSystemException.class).hasMessageContaining("UNIQUE constraint failed");
    assertThat(count("cash_shift")).isEqualTo(1);
  }

  @Test void closedShiftCannotBeClosedOrReceiveMovements() {
    CashShift shift = shifts.openShift(admin);
    shifts.closeShift(shift.getId(), BigDecimal.TEN, null, admin);

    assertThatThrownBy(() -> shifts.closeShift(shift.getId(), BigDecimal.TEN, null, admin))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> shifts.addMovement(shift.getId(), CashMovementType.IN, BigDecimal.ONE, "x", admin))
        .isInstanceOf(IllegalStateException.class);
    assertThat(count("cash_movement")).isZero();
  }

  // ---------- продажа ----------

  @Test void cashSaleIsStoredWithShiftSalesAndTwoDigitMoney() {
    CashShift shift = shifts.openShift(admin);
    saveProduct("111", "Хлеб");
    saveProduct("222", "Молоко");

    receipts.processPayment(cash(41.00, 50.00, 9.00),
        FXCollections.observableArrayList(item("111", 10.00, 3), item("222", 5.50, 2)));

    Map<String, Object> receipt = jdbc.queryForMap("select * from payment_receipt");
    assertThat(receipt.get("status")).isEqualTo("SALE");
    assertThat(receipt.get("payment_type")).isEqualTo("CASH");
    assertThat(((Number) receipt.get("cash_shift_id")).longValue()).isEqualTo(shift.getId());
    assertThat(money(receipt.get("total"))).isEqualByComparingTo("41.00");
    assertThat(money(receipt.get("cash_payment"))).isEqualByComparingTo("41.00");
    assertThat(money(receipt.get("change_money"))).isEqualByComparingTo("9.00");
    assertThat(receipt.get("created_by")).isEqualTo("system");

    var lines = sales.getSalesByPaymentReceipt(receiptRepository.findAll().getFirst());
    assertThat(lines).extracting("barcode", "productName", "quantity")
        .containsExactlyInAnyOrder(
            org.assertj.core.groups.Tuple.tuple("111", "Хлеб", 3),
            org.assertj.core.groups.Tuple.tuple("222", "Молоко", 2));
    assertThat(lines).extracting("total").map(t -> (BigDecimal) t)
        .usingElementComparator(BigDecimal::compareTo)
        .containsExactlyInAnyOrder(new BigDecimal("30.00"), new BigDecimal("11.00"));
  }

  @Test void floatingPointCartTotalsAreRoundedToCents() {
    shifts.openShift(admin);
    saveProduct("111", "Конфета");

    receipts.processPayment(cash(0.70, 0.70, 0.00),
        FXCollections.observableArrayList(item("111", 0.1, 7)));

    assertThat(money(jdbc.queryForObject("select total from sales", Object.class)))
        .isEqualByComparingTo("0.70");
    assertThat(jdbc.queryForObject("select cast(total as text) from sales", String.class))
        .doesNotContain("0.7000000");
  }

  @Test void saleWithoutOpenShiftIsRejectedAndNothingIsSaved() {
    saveProduct("111", "Хлеб");

    assertThatThrownBy(() -> receipts.processPayment(cash(10, 10, 0),
        FXCollections.observableArrayList(item("111", 10.00, 1))))
        .isInstanceOf(BusinessException.class);

    assertThat(count("payment_receipt")).isZero();
    assertThat(count("sales")).isZero();
  }

  @Test void failureWhileSavingASaleLineRollsBackTheWholeReceipt() {
    shifts.openShift(admin);
    saveProduct("111", "Хлеб");
    ProductItem broken = item(null, 10.00, 1); // штрихкод NOT NULL в БД

    assertThatThrownBy(() -> receipts.processPayment(cash(20, 20, 0),
        FXCollections.observableArrayList(item("111", 10.00, 1), broken)))
        .isInstanceOf(JpaSystemException.class).hasMessageContaining("NOT NULL constraint failed");

    assertThat(count("payment_receipt")).isZero();
    assertThat(count("sales")).isZero();
  }

  // ---------- возврат ----------

  @Test void partialReturnIsLinkedToOriginalAndReducesReturnableQuantity() {
    shifts.openShift(admin);
    saveProduct("111", "Хлеб");
    receipts.processPayment(cash(50, 50, 0), FXCollections.observableArrayList(item("111", 10.00, 5)));
    PaymentReceipt original = receipts.getNewestSalePaymentReceipt();

    receipts.processReturnSave(returnCash(20), FXCollections.observableArrayList(item("111", 10.00, 2)), original);

    PaymentReceipt returned = receipts.getReturnPaymentReceipt(original).getFirst();
    assertThat(returned.getReceiptStatus()).isEqualTo(ReceiptStatus.RETURN);
    assertThat(returned.getTotal()).isEqualByComparingTo("-20.00");
    assertThat(jdbc.queryForObject("select original_receipt_id from payment_receipt where id = ?", Long.class,
        returned.getId())).isEqualTo(original.getId());
    assertThat(jdbc.queryForObject("select return_flag from sales where payment_receipt_id = ?", Integer.class,
        returned.getId())).isEqualTo(1);
    assertThat(jdbc.queryForObject("select count(*) from sales where return_date is not null", Integer.class))
        .isEqualTo(1);

    var originalLines = sales.getSalesByPaymentReceipt(original);
    var remaining = receipts.subtractReturnedSales(original, new ArrayList<>(originalLines));
    assertThat(remaining).singleElement().satisfies(l -> {
      assertThat(l.quantity()).isEqualTo(3);
      assertThat(l.total()).isEqualByComparingTo("30.00");
    });

    receipts.processReturnSave(returnCash(30), FXCollections.observableArrayList(item("111", 10.00, 3)), original);
    assertThat(receipts.subtractReturnedSales(original, new ArrayList<>(originalLines))).isEmpty();
  }

  // ---------- отложенные чеки ----------

  @Test void deferredReceiptLifecycleFromCartEditsToFinalPayment() {
    shifts.openShift(admin);
    saveProduct("111", "Хлеб");
    saveProduct("222", "Молоко");
    saveProduct("333", "Сыр");

    PaymentReceipt deferred = receipts.createDeferredPaymentReceipt();
    assertThat(receipts.getDeferredPaymentReceipts()).extracting(PaymentReceipt::getId).containsExactly(deferred.getId());

    receipts.mergeDeferredPaymentReceipts(deferred,
        List.of(item("111", 10.00, 1), item("222", 5.00, 2)));
    assertThat(countSalesOf(deferred)).isEqualTo(2);

    // покупатель убрал молоко и добавил сыр; хлеб остался (имеет salesId) и поменял количество
    Long breadSaleId = jdbc.queryForObject("select id from sales where barcode = '111'", Long.class);
    ProductItem bread = item("111", 10.00, 4);
    bread.setSalesId(breadSaleId);
    receipts.mergeDeferredPaymentReceipts(deferred, List.of(bread, item("333", 20.00, 1)));

    assertThat(jdbc.queryForList("select barcode from sales order by barcode", String.class))
        .containsExactly("111", "333");
    assertThat(jdbc.queryForObject("select quantity from sales where barcode = '111'", Integer.class)).isEqualTo(4);
    assertThat(jdbc.queryForObject("select id from sales where barcode = '111'", Long.class)).isEqualTo(breadSaleId);

    // отложенные чеки не попадают в историю
    assertThat(receipts.findByCreatedDate(LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
        PageRequest.of(0, 50)).getContent()).isEmpty();
    assertThat(receipts.findById(deferred.getId(), PageRequest.of(0, 10)).getContent()).isEmpty();

    // оплата отложенного чека превращает его в продажу, новые строки не дублируются
    ProductItem cheese = item("333", 20.00, 1);
    cheese.setSalesId(jdbc.queryForObject("select id from sales where barcode = '333'", Long.class));
    bread.setQuantity(4);
    receipts.processPaymentSaveWithDeferredPayment(deferred, cash(60, 60, 0),
        FXCollections.observableArrayList(bread, cheese));

    assertThat(jdbc.queryForObject("select status from payment_receipt where id = ?", String.class, deferred.getId()))
        .isEqualTo("SALE");
    assertThat(count("sales")).isEqualTo(2);
    assertThat(receipts.getDeferredPaymentReceipts()).isEmpty();
    assertThat(receipts.findByCreatedDate(LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
        PageRequest.of(0, 50)).getContent()).extracting(PaymentReceipt::getId).containsExactly(deferred.getId());
    assertThat(receipts.findById(deferred.getId(), PageRequest.of(0, 10)).getContent()).hasSize(1);
  }

  @Test void emptyingDeferredCartRemovesItsSalesAndEmptyCartDeletesTheReceipt() {
    shifts.openShift(admin);
    saveProduct("111", "Хлеб");
    PaymentReceipt deferred = receipts.createDeferredPaymentReceipt();
    receipts.mergeDeferredPaymentReceipts(deferred, List.of(item("111", 10.00, 1)));
    assertThat(count("sales")).isEqualTo(1);

    receipts.mergeDeferredPaymentReceipts(deferred, List.of());

    assertThat(count("payment_receipt")).isZero();
    assertThat(count("sales")).isZero(); // продажи удалены каскадом вместе с чеком
  }

  // ---------- закрытие смены ----------

  @Test void closingShiftAggregatesEveryReceiptTypeFromRealSql() {
    CashShift shift = shifts.openShift(admin);
    saveProduct("111", "Хлеб");
    var cart = FXCollections.observableArrayList(item("111", 10.00, 1));

    receipts.processPayment(cash(100, 100, 0), cart);                                        // наличные +100
    receipts.processPayment(card(50), cart);                                                  // карта +50
    receipts.processPayment(new PaymentSumDetails(30, 30, 0, 0, PaymentType.MIXED, 10, 20), cart); // +10 нал, +20 карта
    receipts.processPayment(cash(25, 25, 0), cart);
    PaymentReceipt original = receipts.getNewestSalePaymentReceipt();
    receipts.processReturnSave(returnCash(40), cart, original);                               // возврат наличными −40
    DiaryTransaction debt = new DiaryTransaction("c1", 70);
    receipts.processDebt(new PaymentSumDetails(70), cart, debt);                              // долг +70
    receipts.processReturnSave(returnDebt(20), cart, original);                               // возврат долга −20
    shifts.addMovement(shift.getId(), CashMovementType.IN, new BigDecimal("200.00"), "размен", admin);
    shifts.addMovement(shift.getId(), CashMovementType.OUT, new BigDecimal("50.00"), "инкассация", admin);

    PaymentReceipt pending = receipts.createDeferredPaymentReceipt();
    assertThatThrownBy(() -> shifts.closeShift(shift.getId(), new BigDecimal("300.00"), "x", admin))
        .hasMessageContaining("отложенные");
    assertThat(shiftRepository.findById(shift.getId()).orElseThrow().getStatus()).isEqualTo(CashShiftStatus.OPENED);
    receipts.deleteDeferredPaymentReceipt(pending);

    shifts.closeShift(shift.getId(), new BigDecimal("300.00"), "вечер", admin);

    CashShift closed = shiftRepository.findById(shift.getId()).orElseThrow();
    assertThat(closed.getStatus()).isEqualTo(CashShiftStatus.CLOSED);
    assertThat(closed.getClosedBy().getId()).isEqualTo(admin.getId());
    assertThat(closed.getNote()).isEqualTo("вечер");
    assertThat(closed.getLeftInDrawer()).isEqualByComparingTo("300.00");
    assertThat(closed.getSumCash()).isEqualByComparingTo("95.00");       // 100 + 10 + 25 − 40
    assertThat(closed.getSumCard()).isEqualByComparingTo("70.00");       // 50 + 20
    assertThat(closed.getSumReturnCash()).isEqualByComparingTo("-40.00");
    assertThat(closed.getSumReturnCard()).isEqualByComparingTo("0");
    assertThat(closed.getSumDebt()).isEqualByComparingTo("70.00");
    assertThat(closed.getSumDebtReturn()).isEqualByComparingTo("-20.00");

    ArgumentCaptor<BigDecimal> deposited = ArgumentCaptor.forClass(BigDecimal.class);
    ArgumentCaptor<BigDecimal> withdrawn = ArgumentCaptor.forClass(BigDecimal.class);
    verify(receiptPrintService).printCashShift(any(CashShift.class), deposited.capture(), withdrawn.capture(),
        any(BigDecimal.class), any(BigDecimal.class));
    assertThat(deposited.getValue()).isEqualByComparingTo("200.00");
    assertThat(withdrawn.getValue()).isEqualByComparingTo("50.00");
  }

  @Test void emptyShiftClosesWithZeroTotals() {
    CashShift shift = shifts.openShift(admin);

    shifts.closeShift(shift.getId(), BigDecimal.ZERO, null, admin);

    CashShift closed = shiftRepository.findById(shift.getId()).orElseThrow();
    assertThat(closed.getSumCash()).isEqualByComparingTo("0");
    assertThat(closed.getSumCard()).isEqualByComparingTo("0");
    assertThat(closed.getSumDebt()).isEqualByComparingTo("0");
  }

  @Test void shiftTotalsIgnoreReceiptsOfOtherShifts() {
    CashShift first = shifts.openShift(admin);
    saveProduct("111", "Хлеб");
    receipts.processPayment(cash(100, 100, 0), FXCollections.observableArrayList(item("111", 100.00, 1)));
    shifts.closeShift(first.getId(), BigDecimal.ZERO, null, admin);

    CashShift second = shifts.openShift(admin);
    receipts.processPayment(cash(7, 7, 0), FXCollections.observableArrayList(item("111", 7.00, 1)));
    shifts.closeShift(second.getId(), BigDecimal.ZERO, null, admin);

    assertThat(shiftRepository.findById(first.getId()).orElseThrow().getSumCash()).isEqualByComparingTo("100.00");
    assertThat(shiftRepository.findById(second.getId()).orElseThrow().getSumCash()).isEqualByComparingTo("7.00");
  }

  // ---------- синхронизация 1С ----------

  @Test void categorySyncCreatesThenUpdatesInPlace() {
    writer.applyCategorySync(List.of(new CategoryDto("A", "Молочка", null), new CategoryDto("B", "Хлеб", "A")));
    assertThat(categories.findAllByCategoryCodeIn(Set.of("A", "B"))).hasSize(2);

    var counts = writer.applyCategorySync(List.of(new CategoryDto("A", "Молочные продукты", null),
        new CategoryDto("C", "Напитки", null)));

    assertThat(counts.created()).isEqualTo(1);
    assertThat(counts.updated()).isEqualTo(1);
    assertThat(categories.findAllByCategoryCodeIn(Set.of("A"))).singleElement()
        .satisfies(c -> assertThat(c.getCategoryName()).isEqualTo("Молочные продукты"));
    assertThat(count("category")).isEqualTo(3);
  }

  @Test void productSyncCreatesUpdatesAndSkipsUnknownCategories() {
    writer.applyCategorySync(List.of(new CategoryDto("A", "Молочка", null)));

    var first = writer.applyProductSync(List.of(
        new ProductDto("A", "Молоко", "111", new BigDecimal("250.00")),
        new ProductDto("NOPE", "Призрак", "999", new BigDecimal("1.00")),
        new ProductDto(null, "Без категории", "555", new BigDecimal("3.00"))));

    assertThat(first.created()).isEqualTo(2);
    assertThat(first.skippedNoCategory()).isEqualTo(1);
    assertThat(products.findFirstByBarcode("999")).isEmpty();
    Product milk = products.findFirstByBarcode("111").orElseThrow();
    assertThat(milk.getWholesalePrice()).isEqualByComparingTo("250.00");
    assertThat(milk.getOriginalPrice()).isEqualByComparingTo("0");
    assertThat(jdbc.queryForObject("select category_ref_id from product where barcode = '111'", Long.class))
        .isNotNull();
    assertThat(jdbc.queryForObject("select category_ref_id from product where barcode = '555'", Long.class)).isNull();

    var second = writer.applyProductSync(List.of(new ProductDto("A", "Молоко 3.2%", "111", new BigDecimal("270.00"))));

    assertThat(second.updated()).isEqualTo(1);
    assertThat(second.created()).isZero();
    assertThat(products.findFirstByBarcode("111").orElseThrow().getProductName()).isEqualTo("Молоко 3.2%");
    assertThat(products.findFirstByBarcode("111").orElseThrow().getWholesalePrice()).isEqualByComparingTo("270.00");
  }

  @Test void failedProductSyncRollsBackUpdatesMadeToExistingProducts() {
    writer.applyProductSync(List.of(new ProductDto(null, "Старое имя", "111", new BigDecimal("10.00"))));

    // существующий товар обновляется «на месте», а новый (без оптовой цены) нарушит NOT NULL
    assertThatThrownBy(() -> writer.applyProductSync(List.of(
        new ProductDto(null, "Новое имя", "111", new BigDecimal("20.00")),
        new ProductDto(null, "Битый", "222", null))))
        .isInstanceOf(JpaSystemException.class).hasMessageContaining("NOT NULL constraint failed");

    Product untouched = products.findFirstByBarcode("111").orElseThrow();
    assertThat(untouched.getProductName()).isEqualTo("Старое имя");
    assertThat(untouched.getWholesalePrice()).isEqualByComparingTo("10.00");
    assertThat(products.findFirstByBarcode("222")).isEmpty();
  }

  // ---------- пользователи и настройки ----------

  @Test void createdUserIsStoredWithHashedPasswordThatMatches() {
    userService.createUser(new UserDto(null, "ivan", "secret", "Иван", UserRole.CASHIER, true));

    String stored = jdbc.queryForObject("select password from user where username = 'ivan'", String.class);
    assertThat(stored).isNotEqualTo("secret");
    assertThat(passwordEncoder.matches("secret", stored)).isTrue();
    assertThat(passwordEncoder.matches("wrong", stored)).isFalse();
    assertThat(jdbc.queryForObject("select role from user where username = 'ivan'", String.class)).isEqualTo("CASHIER");
    assertThat(jdbc.queryForObject("select active from user where username = 'ivan'", Integer.class)).isEqualTo(1);
  }

  @Test void duplicateUsernameIsRejectedIgnoringCase() {
    userService.createUser(new UserDto(null, "ivan", "secret", "Иван", UserRole.CASHIER, true));

    assertThatThrownBy(() -> userService.createUser(new UserDto(null, "IVAN", "other", "Другой", UserRole.ADMIN, true)))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(count("user")).isEqualTo(2); // admin + ivan
  }

  @Test void updateKeepsPasswordHashUnlessNewOneIsGiven() {
    userService.createUser(new UserDto(null, "ivan", "secret", "Иван", UserRole.CASHIER, true));
    Long id = jdbc.queryForObject("select id from user where username = 'ivan'", Long.class);
    String hash = jdbc.queryForObject("select password from user where id = ?", String.class, id);

    userService.updateUser(new UserDto(id, "ivan", null, "Иван Петров", UserRole.ADMIN, false));

    assertThat(jdbc.queryForObject("select password from user where id = ?", String.class, id)).isEqualTo(hash);
    assertThat(jdbc.queryForObject("select display_name from user where id = ?", String.class, id)).isEqualTo("Иван Петров");
    assertThat(jdbc.queryForObject("select role from user where id = ?", String.class, id)).isEqualTo("ADMIN");
    assertThat(jdbc.queryForObject("select active from user where id = ?", Integer.class, id)).isEqualTo(0);

    userService.updateUser(new UserDto(id, "ivan", "brand-new", "Иван Петров", UserRole.ADMIN, false));

    String newHash = jdbc.queryForObject("select password from user where id = ?", String.class, id);
    assertThat(newHash).isNotEqualTo(hash);
    assertThat(passwordEncoder.matches("brand-new", newHash)).isTrue();
  }

  @Test void updateCannotTakeUsernameOfAnotherUser() {
    userService.createUser(new UserDto(null, "ivan", "secret", "Иван", UserRole.CASHIER, true));
    userService.createUser(new UserDto(null, "petr", "secret", "Пётр", UserRole.CASHIER, true));
    Long petrId = jdbc.queryForObject("select id from user where username = 'petr'", Long.class);

    assertThatThrownBy(() -> userService.updateUser(new UserDto(petrId, "Ivan", null, "Пётр", UserRole.CASHIER, true)))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(jdbc.queryForObject("select username from user where id = ?", String.class, petrId)).isEqualTo("petr");
  }

  @Test void appSettingsAreCreatedOnceAndThenUpdatedInPlace() {
    assertThat(appSettings.getSingleton()).isEmpty();

    AppSetting first = new AppSetting();
    first.setOrgName("ИП Иванов");
    first.setBin("123456789012");
    first.setSaleStore("Магазин 1");
    first.setCategoryWebAddress("http://1c/categories");
    first.setProductWebAddress("http://1c/products");
    first.setDebtDiaryBaseAddress("http://diary");
    first.setDebtDiaryLogin("cashier");
    first.setDebtDiaryPassword("pwd");
    Long id = appSettings.saveSingleton(first).getId();

    AppSetting second = new AppSetting();
    second.setOrgName("ИП Петров");
    second.setBin("999999999999");
    second.setSaleStore("Магазин 2");
    second.setCategoryWebAddress("http://1c/categories2");
    second.setProductWebAddress("http://1c/products2");
    second.setDebtDiaryBaseAddress("http://diary2");
    second.setDebtDiaryLogin("cashier2");
    second.setDebtDiaryPassword("pwd2");
    appSettings.saveSingleton(second);

    assertThat(count("app_settings")).isEqualTo(1);
    AppSetting stored = appSettings.getSingleton().orElseThrow();
    assertThat(stored.getId()).isEqualTo(id);
    assertThat(stored.getOrgName()).isEqualTo("ИП Петров");
    assertThat(stored.getBin()).isEqualTo("999999999999");
    assertThat(stored.getProductWebAddress()).isEqualTo("http://1c/products2");
    assertThat(stored.getDebtDiaryBaseAddress()).isEqualTo("http://diary2");
    assertThat(stored.getDebtDiaryLogin()).isEqualTo("cashier2");
    assertThat(stored.getDebtDiaryPassword()).isEqualTo("pwd2");
    assertThat(appSettings.loadOrNew().getId()).isEqualTo(id);
  }

  // ---------- helpers ----------

  private int count(String table) {
    return Objects.requireNonNull(jdbc.queryForObject("select count(*) from " + table, Integer.class));
  }

  private int countSalesOf(PaymentReceipt receipt) {
    return Objects.requireNonNull(jdbc.queryForObject(
        "select count(*) from sales where payment_receipt_id = ?", Integer.class, receipt.getId()));
  }

  private static BigDecimal money(Object value) {
    return new BigDecimal(String.valueOf(value));
  }

  private void saveProduct(String barcode, String name) {
    Product p = new Product();
    p.setBarcode(barcode);
    p.setProductName(name);
    p.setOriginalPrice(BigDecimal.TEN);
    p.setWholesalePrice(BigDecimal.ONE);
    products.save(p);
  }

  private static ProductItem item(String barcode, double price, int quantity) {
    ProductItem item = new ProductItem(barcode, "item", price, price);
    item.setQuantity(quantity);
    return item;
  }

  private static PaymentSumDetails cash(double total, double received, double change) {
    return new PaymentSumDetails(total, received, 0, change, PaymentType.CASH, total, 0);
  }

  @SuppressWarnings("SameParameterValue")
  private static PaymentSumDetails card(double total) {
    return new PaymentSumDetails(total, total, 0, 0, PaymentType.CARD, 0, total);
  }

  /** Как ReturnDialogController: суммы возврата хранятся отрицательными. */
  private static PaymentSumDetails returnCash(double amount) {
    return new PaymentSumDetails(-amount, -amount, 0, 0, PaymentType.CASH, -amount, 0);
  }

  @SuppressWarnings("SameParameterValue")
  private static PaymentSumDetails returnDebt(double amount) {
    return new PaymentSumDetails(-amount, -amount, 0, 0, PaymentType.DEBT, 0, 0);
  }

  private CashShift newOpenShift() {
    CashShift s = new CashShift();
    s.setStatus(CashShiftStatus.OPENED);
    s.setShiftOpenedDate(LocalDateTime.now());
    s.setOpenedUser(admin);
    s.setCashDuringOpening(BigDecimal.ZERO);
    return s;
  }

  @AfterAll
  static void cleanup() {
    try {
      Files.deleteIfExists(dbFile);
      Files.deleteIfExists(Path.of(dbFile + "-wal"));
      Files.deleteIfExists(Path.of(dbFile + "-shm"));
    } catch (IOException ignored) {
      // временный файл, ОС уберёт сама
    }
  }
}
