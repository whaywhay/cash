package kz.store.cash.repository;

import java.util.List;
import kz.store.cash.model.entity.Sales;
import kz.store.cash.fx.model.SalesWithProductName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


public interface SalesRepository extends JpaRepository<Sales, Long> {

  @Query("""
          select new kz.store.cash.fx.model.SalesWithProductName(
            s.id,
            s.barcode,
            p.productName,
            s.soldPrice,
            s.originalPrice,
            s.wholesalePrice,
            s.quantity,
            s.total,
            s.returnFlag,
            s.returnDate
          )
          from Sales s
          join Product p on p.barcode = s.barcode
          where s.paymentReceipt.id = :receiptId
      """)
  List<SalesWithProductName> findSalesWithProductNames(Long receiptId);
}
