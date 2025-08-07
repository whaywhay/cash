package kz.store.cash.repository;

import java.time.LocalDateTime;
import java.util.List;
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


  List<PaymentReceipt> getAllByReceiptStatusAndCreatedAfter(ReceiptStatus receiptStatus,
      LocalDateTime createdAfter);
}
