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
import java.time.LocalDateTime;
import kz.store.cash.model.enums.CashShiftStatus;
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
@Table(name = "cash_shift", schema = "main")
public class CashShift extends BaseEntity {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private CashShiftStatus status;

  @Column(name = "shift_opened_date", nullable = false)
  private LocalDateTime shiftOpenedDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "opened_user_id", nullable = false)
  @ToString.Exclude
  private User openedUser;

  @Column(name = "cash_during_opening", nullable = false, precision = 15, scale = 2)
  private BigDecimal cashDuringOpening;

  @Column(name = "shift_closed_date")
  private LocalDateTime shiftClosedDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "closed_by_id")
  @ToString.Exclude
  private User closedBy;

  @Column(name = "sum_cash", precision = 15, scale = 2)
  private BigDecimal sumCash;

  @Column(name = "sum_card", precision = 15, scale = 2)
  private BigDecimal sumCard;

  @Column(name = "left_in_drawer", precision = 15, scale = 2)
  private BigDecimal leftInDrawer;

  @Column(name = "note")
  private String note;

  /* Обратные связи
  @OneToMany(mappedBy = "cashShift", fetch = FetchType.LAZY)
  @ToString.Exclude
  private List<PaymentReceipt> paymentReceipts;

  @OneToMany(mappedBy = "cashShift", fetch = FetchType.LAZY)
  @ToString.Exclude
  private List<CashMovement> cashMovements;

  */

}
