package kz.store.cash;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Проверяет саму механику файловой блокировки от двойного запуска (без JavaFX/Spring — они
 * недоступны в headless-тестах, см. SqliteSchemaIntegrationTest).
 */
class MainCashApplicationLockTest {

  @AfterEach
  void cleanup() {
    MainCashApplication.releaseSingleInstanceLock();
    try {
      Files.deleteIfExists(MainCashApplication.LOCK_FILE);
    } catch (IOException ignored) {
    }
  }

  @Test
  void secondAcquireFailsWhileFirstIsHeld() {
    assertThat(MainCashApplication.acquireSingleInstanceLock()).isTrue();
    assertThat(MainCashApplication.acquireSingleInstanceLock()).isFalse();
  }

  @Test
  void lockCanBeReacquiredAfterRelease() {
    assertThat(MainCashApplication.acquireSingleInstanceLock()).isTrue();
    MainCashApplication.releaseSingleInstanceLock();
    assertThat(MainCashApplication.acquireSingleInstanceLock()).isTrue();
  }
}
