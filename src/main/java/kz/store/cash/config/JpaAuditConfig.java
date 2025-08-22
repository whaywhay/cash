package kz.store.cash.config;

import java.util.Optional;
import kz.store.cash.model.entity.User;
import kz.store.cash.security.CurrentUserStore;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
@RequiredArgsConstructor
public class JpaAuditConfig {

  private final CurrentUserStore currentUserStore;

  @Bean
  public AuditorAware<String> auditorAware() {
    return () -> Optional.ofNullable(currentUserStore.get())
        .map(User::getUsername)
        .or(() -> Optional.of("system"));
  }
}
