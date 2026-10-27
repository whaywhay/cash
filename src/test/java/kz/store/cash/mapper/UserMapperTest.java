package kz.store.cash.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kz.store.cash.model.UserDto;
import kz.store.cash.model.entity.User;
import kz.store.cash.model.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserMapperTest {

  UserMapper mapper = Mappers.getMapper(UserMapper.class);
  PasswordEncoder encoder = mock(PasswordEncoder.class);

  @Test void newUserGetsEncodedPasswordAndNeverThePlainOne() {
    when(encoder.encode("plain")).thenReturn("ENC(plain)");

    User user = mapper.toUser(new UserDto(99L, "ivan", "plain", "Иван", UserRole.ADMIN, true), encoder);

    assertThat(user.getPassword()).isEqualTo("ENC(plain)");
    assertThat(user.getUsername()).isEqualTo("ivan");
    assertThat(user.getDisplayName()).isEqualTo("Иван");
    assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
    assertThat(user.isActive()).isTrue();
  }

  @Test void newUserNeverTakesIdFromDto() {
    when(encoder.encode("p")).thenReturn("ENC");

    User user = mapper.toUser(new UserDto(99L, "ivan", "p", "Иван", UserRole.CASHIER, true), encoder);

    assertThat(user.getId()).isNull();
  }

  @Test void newUserStaysActiveWhenActiveFlagIsNotGiven() {
    when(encoder.encode("p")).thenReturn("ENC");

    User user = mapper.toUser(new UserDto(null, "ivan", "p", "Иван", UserRole.CASHIER, null), encoder);

    assertThat(user.isActive()).isTrue();
  }

  @Test void newInactiveUserIsCreatedInactive() {
    when(encoder.encode("p")).thenReturn("ENC");

    User user = mapper.toUser(new UserDto(null, "ivan", "p", "Иван", UserRole.CASHIER, false), encoder);

    assertThat(user.isActive()).isFalse();
  }

  @Test void updateChangesGivenFieldsAndReEncodesNewPassword() {
    User user = existing();
    when(encoder.encode("new-pass")).thenReturn("ENC(new)");

    mapper.updateToUser(user, new UserDto(5L, "petr2", "new-pass", "Пётр II", UserRole.ADMIN, false), encoder);

    assertThat(user.getUsername()).isEqualTo("petr2");
    assertThat(user.getDisplayName()).isEqualTo("Пётр II");
    assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
    assertThat(user.isActive()).isFalse();
    assertThat(user.getPassword()).isEqualTo("ENC(new)");
  }

  @Test void updateWithoutPasswordKeepsTheStoredHash() {
    User user = existing();

    mapper.updateToUser(user, new UserDto(5L, "petr", null, "Пётр", UserRole.CASHIER, true), encoder);

    assertThat(user.getPassword()).isEqualTo("OLD-HASH");
    verify(encoder, never()).encode(org.mockito.ArgumentMatchers.any());
  }

  @Test void updateWithBlankPasswordKeepsTheStoredHash() {
    User user = existing();

    mapper.updateToUser(user, new UserDto(5L, "petr", "   ", "Пётр", UserRole.CASHIER, true), encoder);

    assertThat(user.getPassword()).isEqualTo("OLD-HASH");
    verify(encoder, never()).encode(org.mockito.ArgumentMatchers.any());
  }

  @Test void updateIgnoresNullFieldsAndNeverChangesId() {
    User user = existing();

    mapper.updateToUser(user, new UserDto(777L, null, null, null, null, null), encoder);

    assertThat(user.getId()).isEqualTo(5L);
    assertThat(user.getUsername()).isEqualTo("petr");
    assertThat(user.getDisplayName()).isEqualTo("Пётр");
    assertThat(user.getRole()).isEqualTo(UserRole.CASHIER);
    assertThat(user.isActive()).isTrue();
  }

  private static User existing() {
    User user = new User("petr", "OLD-HASH", "Пётр", UserRole.CASHIER, true);
    user.setId(5L);
    return user;
  }
}
