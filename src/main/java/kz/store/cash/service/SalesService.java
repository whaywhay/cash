package kz.store.cash.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import kz.store.cash.fx.model.SalesWithProductName;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.model.entity.Sales;
import kz.store.cash.repository.SalesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesService {

  private final SalesRepository salesRepository;

  public List<SalesWithProductName> getSalesByPaymentReceipt(PaymentReceipt paymentReceipt) {
    return salesRepository.findSalesWithProductNames(paymentReceipt.getId());
  }

  public Sales getSaleById(Long salesId) {
    return salesRepository.findById(salesId)
        .orElse(null);
  }

  public void saveSale(Sales sale) {
    salesRepository.save(sale);
  }

  public Optional<Sales> findById(Long salesId) {
    return salesRepository.findById(salesId);
  }

  public void deleteSalesByIdNotInAndPaymentReceipt(PaymentReceipt receipt,
      List<Long> listSalesId) {
    salesRepository.deleteSalesByIdNotInAndPaymentReceipt(listSalesId, receipt);
  }

  public void deleteSalesByPaymentReceipt(PaymentReceipt receipt) {
    salesRepository.deleteSalesByPaymentReceipt(receipt);
  }

  public List<Sales> getSalesByPaymentReceiptIn(Collection<PaymentReceipt> paymentReceipts) {
    return salesRepository.getSalesByPaymentReceiptIn(paymentReceipts);
  }


}
