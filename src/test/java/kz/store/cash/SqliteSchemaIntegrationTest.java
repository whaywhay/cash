package kz.store.cash;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import kz.store.cash.model.entity.Category;
import kz.store.cash.model.entity.Product;
import kz.store.cash.repository.CategoryRepository;
import kz.store.cash.repository.ProductRepository;
import kz.store.cash.repository.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Единственный тест, который поднимает настоящий Spring-контекст на файловой SQLite: реально
 * прогоняет Flyway-миграции (V1__init_schema.sql, V2__seed_universal_product.sql) против
 * настоящей БД. Мокито-тесты сервисов этого не проверяют — ошибку в самих SQL-миграциях
 * (опечатку, некорректный синтаксис, отсутствующую колонку) видно только здесь.
 */
@SpringBootTest(classes = SqliteSchemaTestApplication.class)
class SqliteSchemaIntegrationTest {

  private static Path dbFile;

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) throws IOException {
    dbFile = Files.createTempFile("cash-schema-test-", ".db");
    Files.deleteIfExists(dbFile);
    registry.add("spring.datasource.url",
        () -> "jdbc:sqlite:" + dbFile.toAbsolutePath() + "?foreign_keys=on");
  }

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private CategoryRepository categoryRepository;

  @Test
  void flywaySchemaValidatesAgainstSqliteAndBootstrapDataIsPresent() {
    // Дошли до сюда — значит Flyway применил обе миграции на настоящей SQLite без ошибок.
    assertThat(userRepository.findAll()).extracting("username").containsExactly("admin");
    assertThat(productRepository.findFirstByBarcode("universal-product-200001")).isPresent();
  }

  @Test
  void moneyColumnsRoundTripThroughSqliteWithoutPrecisionLoss() {
    Category category = new Category();
    category.setCategoryCode("BEV");
    category.setCategoryName("Напитки");
    category = categoryRepository.save(category);

    Product product = new Product();
    product.setBarcode("4870000000015");
    product.setProductName("Сок 1л");
    product.setOriginalPrice(new BigDecimal("19.99"));
    product.setWholesalePrice(new BigDecimal("15.50"));
    product.setCategoryRef(category);
    Long id = productRepository.save(product).getId();

    Product reloaded = productRepository.findById(id).orElseThrow();
    assertThat(reloaded.getOriginalPrice()).isEqualByComparingTo("19.99");
    assertThat(reloaded.getWholesalePrice()).isEqualByComparingTo("15.50");
    assertThat(reloaded.getCategoryRef().getId()).isEqualTo(category.getId());
  }

  @AfterAll
  static void cleanup() {
    // На Windows файл SQLite ещё может быть занят пулом соединений в момент завершения теста —
    // это не ошибка теста, поэтому неудачное удаление не должно валить сборку.
    try {
      Files.deleteIfExists(dbFile);
      Files.deleteIfExists(Path.of(dbFile + "-wal"));
      Files.deleteIfExists(Path.of(dbFile + "-shm"));
    } catch (IOException ignored) {
      // временный файл, ОС уберёт сама
    }
  }
}
