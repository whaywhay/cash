package kz.store.cash.security;

import kz.store.cash.model.entity.User;
import kz.store.cash.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserService userService;
  private final PasswordEncoder passwordEncoder;
  private final CurrentUserStore currentUserStore;
  private final ApplicationEventPublisher events;

  public void login(String username, String rawPassword) {
    User user = userService.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

    if (!user.isActive()) {
      throw new IllegalStateException("Пользователь заблокирован");
    }
    if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
      throw new IllegalArgumentException("Неверный пароль");
    }
    currentUserStore.set(user);
    events.publishEvent(new AuthEvents.LoginSuccess(user));
  }

  public void logout() {
    currentUserStore.set(null);
    events.publishEvent(new AuthEvents.Logout(this));
  }
}
