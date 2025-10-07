package kz.store.cash.service.diary;

import java.util.concurrent.Callable;
import kz.store.cash.config.ApiClientsForRestConfig.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DebtDiaryExecutor {

  private final DiaryAuthService auth;
  private final TokenHolder tokenHolder;

  public <T> T call(Callable<T> action) {
    auth.ensureToken();
    try {
      return action.call();
    } catch (UnauthorizedException e) {
      tokenHolder.set(null);
      auth.login();
      try {
        return action.call();
      } catch (UnauthorizedException e2) {
        throw new UnauthorizedException(e2.getMessage());
      } catch (Exception ex2) {
        throw wrap(ex2);
      }
    } catch (Exception ex) {
      throw wrap(ex);
    }
  }

  private RuntimeException wrap(Exception ex) {
    return (ex instanceof RuntimeException re) ? re : new RuntimeException(ex);
  }
}
