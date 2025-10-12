package kz.store.cash.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import kz.store.cash.model.entity.CashShift;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.model.enums.ReceiptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, Long> {

  @Query("""
      SELECT p FROM PaymentReceipt p
      WHERE p.created >= :start and p.created < :end
        and p.receiptStatus != 'PENDING'
        ORDER BY p.created DESC
      """)
  Page<PaymentReceipt> findByCreatedDate(LocalDateTime start, LocalDateTime end, Pageable pageable);

  @Query("""
      SELECT p FROM PaymentReceipt p WHERE p.id = :searchId
            and p.receiptStatus != 'PENDING'
      """)
  Page<PaymentReceipt> findById(Long searchId, Pageable pageable);

  List<PaymentReceipt> getPaymentReceiptByOriginalReceipt(PaymentReceipt originalReceipt);

  List<PaymentReceipt> getAllByReceiptStatusAndCashShift(ReceiptStatus receiptStatus,
      CashShift cashShift);

  PaymentReceipt getFirstByReceiptStatusOrderByIdDesc(ReceiptStatus receiptStatus);

  @Query("""
      select coalesce(sum(pr.cashPayment), 0)
      from PaymentReceipt pr
      where pr.cashShift = :shift
      and (pr.receiptStatus = :sale or pr.receiptStatus = :returnSale)
      """)
  BigDecimal sumCashByShiftAndStatus(CashShift shift, ReceiptStatus sale,
      ReceiptStatus returnSale);

  @Query("""
      select coalesce(sum(pr.cardPayment), 0)
      from PaymentReceipt pr
      where pr.cashShift = :shift
      and (pr.receiptStatus = :sale or pr.receiptStatus = :returnSale)
      """)
  BigDecimal sumCardByShiftAndStatus(CashShift shift, ReceiptStatus sale,
      ReceiptStatus returnSale);

  @Query("""
      select coalesce(sum(pr.cardPayment), 0)
      from PaymentReceipt pr
      where pr.cashShift = :shift
      and pr.receiptStatus = :returnSale
      """)
  BigDecimal sumReturnCardByShiftAndStatus(CashShift shift, ReceiptStatus returnSale);

  @Query("""
      select coalesce(sum(pr.cashPayment), 0)
      from PaymentReceipt pr
      where pr.cashShift = :shift
      and pr.receiptStatus = :returnSale
      """)
  BigDecimal sumReturnCashByShiftAndStatus(CashShift shift, ReceiptStatus returnSale);

  @Query("""
      select coalesce(sum(pr.total), 0)
      from PaymentReceipt pr
      where pr.cashShift = :shift
      and pr.receiptStatus = :debtSale
      """)
  BigDecimal sumDebtTotalByShiftAndStatus(CashShift shift, ReceiptStatus debtSale);

  @Query("""
      select coalesce(sum(pr.total), 0)
      from PaymentReceipt pr
      where pr.cashShift = :shift
      and pr.receiptStatus = :debtReturnSale
      """)
  BigDecimal sumDebtReturnTotalByShiftAndStatus(CashShift shift, ReceiptStatus debtReturnSale);

}
