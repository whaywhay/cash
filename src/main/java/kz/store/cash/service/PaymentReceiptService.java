package kz.store.cash.service;

import static kz.store.cash.model.enums.ReceiptStatus.PENDING;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.collections.ObservableList;
import kz.store.cash.fx.model.SalesWithProductName;
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
  private final SalesService salesService;
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

  @Transactional
  public void processPaymentSaveWithDeferredPayment(PaymentReceipt paymentReceipt,
      PaymentSumDetails paymentSumDetails,
      ObservableList<ProductItem> cart) {
    paymentReceiptMapper.updateToPaymentReceipt(paymentReceipt, paymentSumDetails);
    List<Sales> salesList = cart.stream()
        .map(productIem -> {
          if (productIem.getSalesId() == null) {
            return salesMapper.fromProductItemToSales(productIem, paymentReceipt);
          } else {
            var sale = salesService.getSaleById(productIem.getSalesId());
            if (sale != null) {
              salesMapper.updateToSale(sale, productIem, paymentReceipt);
            } else {
              log.error(
                  "Sale with id {} not found during payment(saving to sales table) deferred receipt",
                  productIem.getSalesId());
            }
            return sale;
          }
        })
        .filter(Objects::nonNull)
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

  public List<SalesWithProductName> subtractReturnedSales(PaymentReceipt receipt,
      List<SalesWithProductName> originalSales) {
    List<PaymentReceipt> returnedReceipts = getReturnPaymentReceipt(receipt);
    if (returnedReceipts == null || returnedReceipts.isEmpty()) {
      return originalSales;
    }

    Map<String, Integer> returnedMap = salesService.getSalesByPaymentReceiptIn(returnedReceipts)
        .stream()
        .collect(Collectors.groupingBy(
            s -> s.getBarcode() + "::" + s.getSoldPrice(),
            Collectors.summingInt(Sales::getQuantity)
        ));
    return originalSales.stream()
        .map(sale -> {
          String key = sale.barcode() + "::" + sale.soldPrice();
          int returnedQty = returnedMap.getOrDefault(key, 0);
          int remainingQty = sale.quantity() - returnedQty;

          if (remainingQty <= 0) {
            return null;
          }

          BigDecimal newTotal = sale.soldPrice().multiply(BigDecimal.valueOf(remainingQty));
          return new SalesWithProductName(
              sale.id(),
              sale.barcode(),
              sale.productName(),
              sale.soldPrice(),
              sale.originalPrice(),
              sale.wholesalePrice(),
              remainingQty,
              newTotal,
              sale.returnFlag(),
              sale.returnDate()
          );
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  @Transactional
  public void mergeDeferredPaymentReceipts(PaymentReceipt receipt, List<ProductItem> currentCart) {
    if (currentCart == null || currentCart.isEmpty()) {
      if (receipt != null) {
        deleteDeferredPaymentReceipt(receipt);
      }
      return;
    }
    if (receipt == null) {
      receipt = createDeferredPaymentReceipt();
    }
    List<Long> listSalesId = currentCart.stream().map(ProductItem::getSalesId)
        .filter(Objects::nonNull)
        .toList();
    if (listSalesId.isEmpty()) {
      salesService.deleteSalesByPaymentReceipt(receipt); // удалить все
    } else {
      salesService.deleteSalesByIdNotInAndPaymentReceipt(receipt, listSalesId);
    }
    for (ProductItem productItem : currentCart) {
      if (productItem.getSalesId() != null) {
        var saleOpt = salesService.findById(productItem.getSalesId());
        PaymentReceipt finalReceipt = receipt;
        saleOpt.ifPresentOrElse(sale -> {
          salesMapper.updateToSale(sale, productItem, finalReceipt);
          salesService.saveSale(sale);
        }, () -> {
          throw new RuntimeException("sale not found by id:  " + productItem.getSalesId());
        });
      } else {
        salesService.saveSale(salesMapper.fromProductItemToSales(productItem, receipt));
      }
    }
  }
}
