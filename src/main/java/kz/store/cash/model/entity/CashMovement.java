package kz.store.cash.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import kz.store.cash.model.enums.CashMovementType;
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
@Table(name = "cash_movement", schema = "main")
public class CashMovement extends BaseEntity {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cash_shift_id", nullable = false)
  @ToString.Exclude
  private CashShift cashShift;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private CashMovementType type;

  @Column(name = "amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @Column(name = "reason")
  private String reason;

  /**
   * Кто сделал действие (ссылка на main."user".id)
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "created_by_id", nullable = false)
  private User createdByUser;
}
