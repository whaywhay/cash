package kz.store.cash.service.diary;

import java.util.concurrent.atomic.AtomicReference;

public class TokenHolder {

  private final AtomicReference<String> token = new AtomicReference<>();

  public void set(String t) {
    token.set(t);
  }

  public String get() {
    return token.get();
  }

  public boolean isPresent() {
    return get() != null && !get().isBlank();
  }

  public void clear() {
    token.set(null);
  }
}
