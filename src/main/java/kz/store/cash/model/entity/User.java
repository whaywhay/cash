package kz.store.cash.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kz.store.cash.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "user", schema = "main")
public class User extends BaseEntity {
  public User(
      String username,
      String password,
      String displayName,
      UserRole role,
      boolean active
  ) {
    this.username = username;
    this.password = password;
    this.displayName = displayName;
    this.role = role;
    this.active = active;
  }
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "username", nullable = false, unique = true)
  private String username;

  @Column(name = "password", nullable = false)
  private String password;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private UserRole role = UserRole.CASHIER;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  /* Обратные связи
  @OneToMany(mappedBy = "openedUser", fetch = FetchType.LAZY)
  @ToString.Exclude
  private List<CashShift> openedShifts;

  @OneToMany(mappedBy = "closedBy", fetch = FetchType.LAZY)
  @ToString.Exclude
  private List<CashShift> closedShifts;

  @OneToMany(mappedBy = "createdByUser", fetch = FetchType.LAZY)
  @ToString.Exclude
  private List<CashMovement> cashMovements;
  */
}
