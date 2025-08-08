package kz.store.cash.service;

import static kz.store.cash.model.enums.ReceiptStatus.PENDING;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javafx.collections.ObservableList;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.model.entity.Sales;
import kz.store.cash.fx.model.PaymentSumDetails;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.mapper.PaymentReceiptMapper;
import kz.store.cash.mapper.SalesMapper;
import kz.store.cash.repository.PaymentReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReceiptService {

  private final PaymentReceiptMapper paymentReceiptMapper;
  private final PaymentReceiptRepository paymentReceiptRepository;
  private final SalesMapper salesMapper;

  @Transactional
  public void processPaymentSave(PaymentSumDetails paymentSumDetails,
      ObservableList<ProductItem> cart) {
    PaymentReceipt paymentReceipt = paymentReceiptMapper.toPaymentReceipt(paymentSumDetails);
    List<Sales> salesList = cart.stream()
        .map(sale -> salesMapper.fromProductItemToSales(sale, paymentReceipt))
        .toList();
    saveSalesWithPaymentRecipe(paymentReceipt, salesList);
  }

  private void saveSalesWithPaymentRecipe(PaymentReceipt paymentReceipt, List<Sales> salesList) {
    paymentReceipt.setSalesList(salesList);
    paymentReceiptRepository.save(paymentReceipt);
  }

  @Transactional
  public void processReturnSave(PaymentSumDetails paymentSumDetails,
      ObservableList<ProductItem> cart, PaymentReceipt paymentOriginal) {
    PaymentReceipt paymentReceipt = paymentReceiptMapper.toReturnPaymentReceipt(paymentSumDetails,
        paymentOriginal);
    List<Sales> salesList = cart.stream()
        .map(sale -> salesMapper.fromProductItemToReturnSales(sale, paymentReceipt))
        .toList();
    paymentReceipt.setSalesList(salesList);
    paymentReceiptRepository.save(paymentReceipt);
  }

  public List<PaymentReceipt> getReturnPaymentReceipt(PaymentReceipt paymentReceipt) {
    return paymentReceiptRepository.getPaymentReceiptByOriginalReceipt(paymentReceipt);
  }

  public List<PaymentReceipt> getDeferredPaymentReceipts() {
    LocalDateTime todayAtMidnight = LocalDate.now().atStartOfDay();
    return paymentReceiptRepository.getAllByReceiptStatusAndCreatedAfter(PENDING, todayAtMidnight);
  }

  public PaymentReceipt createDeferredPaymentReceipt() {
    PaymentReceipt paymentReceipt = new PaymentReceipt();
    paymentReceipt.setReceiptStatus(PENDING);
    return paymentReceiptRepository.save(paymentReceipt);
  }

  public void deleteDeferredPaymentReceipt(PaymentReceipt paymentReceipt) {
    paymentReceiptRepository.deleteById(paymentReceipt.getId());
  }

  public Page<PaymentReceipt> findByCreatedDate(LocalDateTime start, LocalDateTime end,
      Pageable pageable) {
    return paymentReceiptRepository.findByCreatedDate(start, end, pageable);
  }

  public Page<PaymentReceipt> findById(Long searchId, Pageable pageable) {
    return paymentReceiptRepository.findById(searchId, pageable);
  }
}
