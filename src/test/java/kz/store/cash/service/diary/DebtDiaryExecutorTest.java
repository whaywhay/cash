package kz.store.cash.service.diary;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import kz.store.cash.config.ApiClientsForRestConfig.UnauthorizedException;
import org.junit.jupiter.api.Test;

class DebtDiaryExecutorTest {
  private final DiaryDebtAuthService auth = mock(DiaryDebtAuthService.class);
  private final TokenHolder holder = mock(TokenHolder.class);
  private final DebtDiaryExecutor executor = new DebtDiaryExecutor(auth, holder);

  @Test void executesAfterEnsuringToken() {
    assertThat(executor.call(() -> "ok")).isEqualTo("ok");
    verify(auth).ensureToken();
  }

  @Test void reauthenticatesAndRetriesOnceOnUnauthorized() {
    AtomicInteger calls = new AtomicInteger();
    String result = executor.call(() -> { if (calls.getAndIncrement() == 0) throw new UnauthorizedException("401"); return "ok"; });
    assertThat(result).isEqualTo("ok");
    verify(holder).set(null); verify(auth).login();
  }

  @Test void wrapsCheckedException() {
    assertThatThrownBy(() -> executor.call(() -> { throw new IOException("io"); }))
        .isInstanceOf(RuntimeException.class).hasCauseInstanceOf(IOException.class);
  }
}
