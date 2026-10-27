package kz.store.cash;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

/**
 * Test-only контекст для {@link SqliteSchemaIntegrationTest}: сканирует только слой, нужный
 * для проверки БД (конфиг, сервисы, репозитории, мапперы, security), без JavaFX-контроллеров и
 * диалогов (fx.controllers/fx.dialog) — они требуют инициализированный JavaFX Toolkit, которого
 * в headless JUnit-запуске нет. Этого достаточно, чтобы реально прогнать Flyway-миграции и
 * убедиться, что схема SQLite соответствует сущностям.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ConfigurationPropertiesScan(basePackages = "kz.store.cash")
@ComponentScan(basePackages = {
    "kz.store.cash.config",
    "kz.store.cash.handler",
    "kz.store.cash.mapper",
    "kz.store.cash.repository",
    "kz.store.cash.security",
    "kz.store.cash.service",
    "kz.store.cash.util",
    "kz.store.cash.fx.component",
})
public class SqliteSchemaTestApplication {
}
