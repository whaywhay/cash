package kz.store.cash.service.diary;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class TokenHolderTest {
  @Test void storesAndClearsToken() {
    TokenHolder holder = new TokenHolder();
    assertThat(holder.isPresent()).isFalse();
    holder.set("token"); assertThat(holder.get()).isEqualTo("token"); assertThat(holder.isPresent()).isTrue();
    holder.clear(); assertThat(holder.isPresent()).isFalse();
    holder.set("  "); assertThat(holder.isPresent()).isFalse();
  }
}
