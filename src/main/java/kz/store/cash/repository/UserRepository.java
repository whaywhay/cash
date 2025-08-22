package kz.store.cash.repository;

import java.util.Optional;
import kz.store.cash.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsername(String username);

  boolean existsByUsernameIgnoreCase(String username);

  boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

}
