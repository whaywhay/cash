package kz.store.cash.repository;

import java.util.Collection;
import java.util.List;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.model.entity.Sales;
import kz.store.cash.fx.model.SalesWithProductName;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;


public interface SalesRepository extends CrudRepository<Sales, Long> {

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

  List<Sales> getSalesByPaymentReceiptIn(Collection<PaymentReceipt> paymentReceipts);

  void deleteSalesByIdNotInAndPaymentReceipt(Collection<Long> ids, PaymentReceipt paymentReceipt);

  void deleteSalesByPaymentReceipt(PaymentReceipt paymentReceipt);

}
