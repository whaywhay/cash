package kz.store.cash.service;

import java.util.Optional;
import kz.store.cash.model.entity.User;
import kz.store.cash.model.enums.UserRole;
import kz.store.cash.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder pe;

  public User save(User user) {
    return userRepository.save(user);
  }

  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }



  public User createUser(String username, String displayName, String rawPassword, UserRole role, boolean active) {
    if (userRepository.existsByUsername(username)) {
      throw new IllegalArgumentException("Логин уже существует");
    }
    User u = new User();
    u.setUsername(username);
    u.setDisplayName(displayName);
    u.setPassword(pe.encode(rawPassword));
    u.setRole(role == null ? UserRole.CASHIER : role);
    u.setActive(active);
    return userRepository.save(u);
  }

  public User updateUser(Long id, String displayName, String newRawPassword, UserRole role, Boolean active) {
    User u = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    if (displayName != null) u.setDisplayName(displayName);
    if (role != null) u.setRole(role);
    if (active != null) u.setActive(active);
    if (newRawPassword != null && !newRawPassword.isBlank()) {
      u.setPassword(pe.encode(newRawPassword));
    }
    return userRepository.save(u);
  }
}
