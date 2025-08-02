package kz.store.cash.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import kz.store.cash.model.enums.PaymentType;
import kz.store.cash.model.enums.ReceiptStatus;
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
@Table(name = "payment_receipt", schema = "main")
public class PaymentReceipt extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "return_date")
  private LocalDateTime returnDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_type")
  private PaymentType paymentType;

  @Column(name = "cash_payment")
  private BigDecimal cashPayment;

  @Column(name = "card_payment")
  private BigDecimal cardPayment;

  @Column(name = "change_money")
  private BigDecimal changeMoney;

  @Column(name = "remaining_payment")
  private BigDecimal remainingPayment;

  @Column(name = "received_payment")
  private BigDecimal receivedPayment;

  @Column(name = "total")
  private BigDecimal total;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private ReceiptStatus receiptStatus;

  /** Ссылка на исходный чек (для возвратов по чеку) */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "original_receipt_id")
  @ToString.Exclude
  private PaymentReceipt originalReceipt;

  @OneToMany(mappedBy = "paymentReceipt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @ToString.Exclude
  private List<Sales> salesList;

  /** Возвратные чеки, которые ссылаются на этот чек */
  @OneToMany(mappedBy = "originalReceipt", fetch = FetchType.LAZY)
  @ToString.Exclude
  private List<PaymentReceipt> returnReceipts;
}