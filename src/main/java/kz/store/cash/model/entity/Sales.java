package kz.store.cash.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.ToString.Exclude;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "sales", schema = "main")
public class Sales extends BaseEntity {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "barcode")
  private String barcode;

  @Column(name = "sold_price")
  private BigDecimal soldPrice;

  @Column(name = "original_price")
  private BigDecimal originalPrice;

  @Column(name = "wholesale_price")
  private BigDecimal wholesalePrice;

  @Column(name = "quantity")
  private int quantity;

  @Column(name = "total")
  private BigDecimal total;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "payment_receipt_id")
  @Exclude
  private PaymentReceipt paymentReceipt;

  @Column(name = "return_flag")
  private boolean returnFlag;

  @Column(name = "return_date")
  private LocalDateTime returnDate;

  /**
   * Ссылка на оригинальную строку продажи (если это возврат по чеку)
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "original_sale_id")
  @ToString.Exclude
  private Sales originalSale;

  /**
   * Список возвратных строк, которые ссылаются на эту продажу
   */
  @OneToMany(mappedBy = "originalSale", fetch = FetchType.LAZY)
  @ToString.Exclude
  private List<Sales> returnSales;
}
