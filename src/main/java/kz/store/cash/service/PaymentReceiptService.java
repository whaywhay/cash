package kz.store.cash.service;

import static kz.store.cash.model.enums.ReceiptStatus.DEBT_RECEIPT;
import static kz.store.cash.model.enums.ReceiptStatus.DEBT_RECEIPT_RETURN;
import static kz.store.cash.model.enums.ReceiptStatus.PENDING;
import static kz.store.cash.model.enums.ReceiptStatus.RETURN;
import static kz.store.cash.model.enums.ReceiptStatus.SALE;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.collections.ObservableList;
import kz.store.cash.fx.model.SalesWithProductName;
import kz.store.cash.model.diarydebt.DiaryTransaction;
import kz.store.cash.model.entity.CashShift;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.model.entity.Sales;
import kz.store.cash.fx.model.PaymentSumDetails;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.mapper.PaymentReceiptMapper;
import kz.store.cash.mapper.SalesMapper;
import kz.store.cash.model.enums.CashShiftStatus;
import kz.store.cash.model.enums.ReceiptStatus;
import kz.store.cash.repository.CashShiftRepository;
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
  private final CashShiftRepository cashShiftRepository;
  private final SalesService salesService;
  private final SalesMapper salesMapper;

  private CashShift getOpenedCashShift() {
    return cashShiftRepository.findFirstByStatusOrderByShiftOpenedDateDesc(CashShiftStatus.OPENED)
        .orElseThrow(() -> new RuntimeException("Нет открытой смены"));
  }

  @Transactional
  public void processPayment(PaymentSumDetails paymentSumDetails,
      ObservableList<ProductItem> cart) {
    PaymentReceipt paymentReceipt = paymentReceiptMapper.saleToPaymentReceipt(paymentSumDetails);
    paymentAndDebtSave(cart, paymentReceipt);
  }

  @Transactional
  public void processDebt(PaymentSumDetails paymentSumDetails,
      ObservableList<ProductItem> cart, DiaryTransaction diaryTransaction) {
    PaymentReceipt paymentReceipt = paymentReceiptMapper.debtToPaymentReceipt(paymentSumDetails,
        diaryTransaction);
    paymentAndDebtSave(cart, paymentReceipt);
  }

  private void paymentAndDebtSave(ObservableList<ProductItem> cart, PaymentReceipt paymentReceipt) {
    List<Sales> salesList = cart.stream()
        .map(sale -> salesMapper.fromProductItemToSales(sale, paymentReceipt))
        .toList();
    saveSalesWithPaymentRecipe(paymentReceipt, salesList);
  }


  @Transactional
  public void processDebtWithDeferredPayment(PaymentReceipt paymentReceipt,
      PaymentSumDetails paymentSumDetails,
      ObservableList<ProductItem> cart, DiaryTransaction diaryTransaction) {
    paymentReceiptMapper.updateDebtToPaymentReceipt(paymentReceipt, paymentSumDetails,
        diaryTransaction);
    debtAndPaymentSave(paymentReceipt, cart);
  }

  @Transactional
  public void processPaymentSaveWithDeferredPayment(PaymentReceipt paymentReceipt,
      PaymentSumDetails paymentSumDetails,
      ObservableList<ProductItem> cart) {
    paymentReceiptMapper.updateDeferredToPaymentReceipt(paymentReceipt, paymentSumDetails);
    debtAndPaymentSave(paymentReceipt, cart);
  }

  private void debtAndPaymentSave(PaymentReceipt paymentReceipt, ObservableList<ProductItem> cart) {
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
    paymentReceipt.setCashShift(getOpenedCashShift());//Подвязка оплаты к открытой смене
    paymentReceipt.setSalesList(salesList);//Продажу подвязать к чеку
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
    paymentReceipt.setCashShift(getOpenedCashShift());//Подвязка открытой смены к оплатам
    paymentReceiptRepository.save(paymentReceipt);
  }

  public List<PaymentReceipt> getReturnPaymentReceipt(PaymentReceipt paymentReceipt) {
    return paymentReceiptRepository.getPaymentReceiptByOriginalReceipt(paymentReceipt);
  }

  public List<PaymentReceipt> getDeferredPaymentReceipts() {
    var cashShift = getOpenedCashShift();
    return paymentReceiptRepository.getAllByReceiptStatusAndCashShift(PENDING, cashShift);
  }

  public PaymentReceipt createDeferredPaymentReceipt() {
    PaymentReceipt paymentReceipt = new PaymentReceipt();
    paymentReceipt.setReceiptStatus(PENDING);
    paymentReceipt.setCashShift(getOpenedCashShift());//Подвязка открытой смены к оплатам
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

  public PaymentReceipt getNewestSalePaymentReceipt() {
    return paymentReceiptRepository.getFirstByReceiptStatusOrderByIdDesc(ReceiptStatus.SALE);
  }

  public BigDecimal getSumCash(CashShift cashShift) {
    return paymentReceiptRepository.sumCashByShiftAndStatus(cashShift, SALE, RETURN);
  }

  public BigDecimal getSumCard(CashShift cashShift) {
    return paymentReceiptRepository.sumCardByShiftAndStatus(cashShift, SALE, RETURN);
  }

  public BigDecimal getReturnedSumCash(CashShift cashShift) {
    return paymentReceiptRepository.sumReturnCashByShiftAndStatus(cashShift, RETURN);
  }

  public BigDecimal getReturnedSumCard(CashShift cashShift) {
    return paymentReceiptRepository.sumReturnCardByShiftAndStatus(cashShift, RETURN);
  }

  public BigDecimal getDebtSum(CashShift cashShift) {
    return paymentReceiptRepository.sumDebtTotalByShiftAndStatus(cashShift, DEBT_RECEIPT);
  }

  public BigDecimal getDebtReturnSum(CashShift cashShift) {
    return paymentReceiptRepository.sumDebtReturnTotalByShiftAndStatus(cashShift, DEBT_RECEIPT_RETURN);
  }

}
