package kz.store.cash.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import java.math.BigDecimal;
import kz.store.cash.entity.PaymentReceipt;
import kz.store.cash.fx.model.PaymentSumDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentReceiptMapper {

  @Mapping(target = "returnDate", ignore = true)
  @Mapping(target = "paymentType", source = "paymentType")
  @Mapping(target = "cashPayment", source = "cashPayment", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "cardPayment", source = "cardPayment", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "changeMoney", source = "changeMoney", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "remainingPayment", source = "remainingPayment", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "receivedPayment", source = "receivedPayment", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "total", source = "totalToPay", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "receiptStatus", constant = "SALE")
  PaymentReceipt toPaymentReceipt(PaymentSumDetails paymentSumDetails);

  @Named("doubleToBigDecimal")
  static BigDecimal mapDoubleToBigDecimal(double value) {
    return BigDecimal.valueOf(value);
  }
}
