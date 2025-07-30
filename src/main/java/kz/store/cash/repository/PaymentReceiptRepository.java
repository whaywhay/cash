package kz.store.cash.repository;

import java.time.LocalDateTime;
import kz.store.cash.entity.PaymentReceipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, Long> {

  @Query("""
      SELECT p FROM PaymentReceipt p
      WHERE p.created >= :start and p.created < :end ORDER BY p.created DESC
      """)
  Page<PaymentReceipt> findByCreatedDate(LocalDateTime start, LocalDateTime end, Pageable pageable);
}
