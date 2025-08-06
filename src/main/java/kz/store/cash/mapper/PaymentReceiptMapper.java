package kz.store.cash.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.fx.model.PaymentSumDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE, imports = {
    LocalDateTime.class})
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

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "created", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "lastUpdated", ignore = true)
  @Mapping(target = "lastUpdatedBy", ignore = true)
  @Mapping(target = "salesList", ignore = true)
  @Mapping(target = "returnReceipts", ignore = true)
  @Mapping(target = "returnDate", expression = "java(LocalDateTime.now())")
  @Mapping(target = "paymentType", source = "paymentSumDetails.paymentType")
  @Mapping(target = "cashPayment", source = "paymentSumDetails.cashPayment", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "cardPayment", source = "paymentSumDetails.cardPayment", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "changeMoney", source = "paymentSumDetails.changeMoney", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "remainingPayment", source = "paymentSumDetails.remainingPayment", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "receivedPayment", source = "paymentSumDetails.receivedPayment", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "total", source = "paymentSumDetails.totalToPay", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "receiptStatus", constant = "RETURN")
  @Mapping(target = "originalReceipt", source = "originalReceipt")
  PaymentReceipt toReturnPaymentReceipt(PaymentSumDetails paymentSumDetails,
      PaymentReceipt originalReceipt);

  @Named("doubleToBigDecimal")
  static BigDecimal mapDoubleToBigDecimal(double value) {
    return BigDecimal.valueOf(value);
  }
}
