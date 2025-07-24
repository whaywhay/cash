package kz.store.cash.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import java.math.BigDecimal;
import kz.store.cash.entity.PaymentReceipt;
import kz.store.cash.entity.Sales;
import kz.store.cash.fx.model.ProductItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SalesMapper {

  @Mapping(target = "returnDate", ignore = true)
  @Mapping(target = "soldPrice", source = "productItem.price", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "originalPrice", source = "productItem.originalPrice", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "wholesalePrice", source = "productItem.wholesalePrice", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "total", source = "productItem.total", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "paymentReceipt", source = "paymentReceipt")
  Sales fromProductItemToSales(ProductItem productItem, PaymentReceipt paymentReceipt);

  @Named("doubleToBigDecimal")
  static BigDecimal mapDoubleToBigDecimal(double value) {
    return BigDecimal.valueOf(value);
  }
}
