package kz.store.cash.service;

import java.util.List;
import java.util.Optional;
import kz.store.cash.mapper.UserMapper;
import kz.store.cash.model.UserDto;
import kz.store.cash.model.entity.User;
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
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;

  public User save(User user) {
    return userRepository.save(user);
  }

  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  public void createUser(UserDto userDto) {
    if (userRepository.existsByUsernameIgnoreCase(userDto.username())) {
      throw new IllegalArgumentException("Логин уже существует");
    }
    User user = userMapper.toUser(userDto, passwordEncoder);
    userRepository.save(user);
  }

  public void updateUser(UserDto userDto) {
    User user = userRepository.findById(userDto.id())
        .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    if (userRepository.existsByUsernameIgnoreCaseAndIdNot(userDto.username(), userDto.id())) {
      throw new IllegalArgumentException("Логин уже используется другим пользователем");
    }
    userMapper.updateToUser(user, userDto, passwordEncoder);
    userRepository.save(user);
  }

  public List<User> findAll() {
    return userRepository.findAll();
  }

  public void deleteUser(Long id) {
    userRepository.deleteById(id);
  }
}
