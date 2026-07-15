package kz.store.cash.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.Optional;
import kz.store.cash.model.entity.User;
import kz.store.cash.model.enums.UserRole;
import kz.store.cash.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
  @Mock UserService userService;
  @Mock PasswordEncoder encoder;
  @Mock ApplicationEventPublisher events;
  CurrentUserStore store;
  AuthService service;

  @BeforeEach void setUp() {
    store = new CurrentUserStore();
    service = new AuthService(userService, encoder, store, events);
  }

  @Test void logsInActiveUserWithValidPassword() {
    User user = user(true);
    when(userService.findByUsername("cashier")).thenReturn(Optional.of(user));
    when(encoder.matches("secret", "hash")).thenReturn(true);

    service.login("cashier", "secret");

    assertThat(store.get()).isSameAs(user);
    verify(events).publishEvent(any(AuthEvents.LoginSuccess.class));
  }

  @Test void rejectsUnknownUserWithoutCheckingPassword() {
    when(userService.findByUsername("unknown")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.login("unknown", "secret"))
        .isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(encoder, events);
    assertThat(store.isLoggedIn()).isFalse();
  }

  @Test void rejectsBlockedUser() {
    when(userService.findByUsername("cashier")).thenReturn(Optional.of(user(false)));
    assertThatThrownBy(() -> service.login("cashier", "secret"))
        .isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(encoder, events);
  }

  @Test void rejectsInvalidPassword() {
    when(userService.findByUsername("cashier")).thenReturn(Optional.of(user(true)));
    when(encoder.matches("wrong", "hash")).thenReturn(false);
    assertThatThrownBy(() -> service.login("cashier", "wrong"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(store.isLoggedIn()).isFalse();
  }

  @Test void logoutClearsCurrentUserAndPublishesEvent() {
    store.set(user(true));
    service.logout();
    assertThat(store.get()).isNull();
    verify(events).publishEvent(any(AuthEvents.Logout.class));
  }

  private User user(boolean active) {
    return new User("cashier", "hash", "Cashier", UserRole.CASHIER, active);
  }
}
