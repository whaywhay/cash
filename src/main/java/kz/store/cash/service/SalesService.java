package kz.store.cash.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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

  private final PaymentReceiptService paymentReceiptService;
  private final SalesRepository salesRepository;

  public List<SalesWithProductName> subtractReturnedSales(PaymentReceipt receipt,
      List<SalesWithProductName> originalSales) {
    List<PaymentReceipt> returnedReceipts = paymentReceiptService.getReturnPaymentReceipt(receipt);
    if (returnedReceipts == null || returnedReceipts.isEmpty()) {
      return originalSales;
    }

    Map<String, Integer> returnedMap = salesRepository.getSalesByPaymentReceiptIn(returnedReceipts)
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

}
