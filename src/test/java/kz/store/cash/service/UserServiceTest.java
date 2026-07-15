package kz.store.cash.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import kz.store.cash.mapper.UserMapper;
import kz.store.cash.model.UserDto;
import kz.store.cash.model.entity.User;
import kz.store.cash.model.enums.UserRole;
import kz.store.cash.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest {
  private final UserRepository repository = mock(UserRepository.class);
  private final PasswordEncoder encoder = mock(PasswordEncoder.class);
  private final UserMapper mapper = mock(UserMapper.class);
  private final UserService service = new UserService(repository, encoder, mapper);

  @Test void createsUniqueUser() {
    UserDto dto = dto(null, "new"); User user = new User();
    when(mapper.toUser(dto, encoder)).thenReturn(user);
    service.createUser(dto);
    verify(repository).save(user);
  }

  @Test void rejectsDuplicateUsername() {
    UserDto dto = dto(null, "used");
    when(repository.existsByUsernameIgnoreCase("used")).thenReturn(true);
    assertThatThrownBy(() -> service.createUser(dto)).isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(mapper);
  }

  @Test void updatesExistingUniqueUser() {
    UserDto dto = dto(4L, "updated"); User user = new User();
    when(repository.findById(4L)).thenReturn(Optional.of(user));
    service.updateUser(dto);
    verify(mapper).updateToUser(user, dto, encoder);
    verify(repository).save(user);
  }

  @Test void rejectsMissingAndConflictingUpdates() {
    UserDto missing = dto(1L, "x");
    when(repository.findById(1L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.updateUser(missing)).isInstanceOf(IllegalArgumentException.class);
    UserDto conflict = dto(2L, "used"); User user = new User();
    when(repository.findById(2L)).thenReturn(Optional.of(user));
    when(repository.existsByUsernameIgnoreCaseAndIdNot("used", 2L)).thenReturn(true);
    assertThatThrownBy(() -> service.updateUser(conflict)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test void basicCrudDelegates() {
    User user = new User(); service.save(user); service.findByUsername("u"); service.findAll(); service.deleteUser(3L);
    verify(repository).save(user); verify(repository).findByUsername("u"); verify(repository).findAll(); verify(repository).deleteById(3L);
  }
  private UserDto dto(Long id, String name) { return new UserDto(id, name, "p", "User", UserRole.CASHIER, true); }
}
