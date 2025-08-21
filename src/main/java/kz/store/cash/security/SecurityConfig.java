package kz.store.cash.security;

import kz.store.cash.model.entity.User;
import kz.store.cash.model.enums.UserRole;
import kz.store.cash.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(); // bcrypt со встроенной солью
  }

  // На первый запуск — создаём админа, если нет ни одного пользователя
  @Bean
  CommandLineRunner bootstrapAdmin(UserRepository userRep, PasswordEncoder pe) {
    return args -> {
      if (userRep.count() == 0) {
        User admin = new  User();
        admin.setUsername("admin");
        admin.setPassword(pe.encode("admin"));
        admin.setDisplayName("Администратор");
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        userRep.save(admin);
      }
    };
  }
}