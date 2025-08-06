package kz.store.cash.service;

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
}
