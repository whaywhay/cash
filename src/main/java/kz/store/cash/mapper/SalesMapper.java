package kz.store.cash.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import kz.store.cash.model.entity.PaymentReceipt;
import kz.store.cash.model.entity.Sales;
import kz.store.cash.fx.model.ProductItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE, imports = {
    LocalDateTime.class})
public interface SalesMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "created", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "lastUpdated", ignore = true)
  @Mapping(target = "lastUpdatedBy", ignore = true)
  @Mapping(target = "returnDate", ignore = true)
  @Mapping(target = "soldPrice", source = "productItem.price", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "originalPrice", source = "productItem.originalPrice", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "wholesalePrice", source = "productItem.wholesalePrice", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "total", source = "productItem.total", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "paymentReceipt", source = "paymentReceipt")
  Sales fromProductItemToSales(ProductItem productItem, PaymentReceipt paymentReceipt);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "created", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "lastUpdated", ignore = true)
  @Mapping(target = "lastUpdatedBy", ignore = true)
  @Mapping(target = "returnDate", expression = "java(LocalDateTime.now())")
  @Mapping(target = "returnFlag", constant = "true")
  @Mapping(target = "soldPrice", source = "productItem.price", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "originalPrice", source = "productItem.originalPrice", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "wholesalePrice", source = "productItem.wholesalePrice", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "total", source = "productItem.total", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "paymentReceipt", source = "paymentReceipt")
  Sales fromProductItemToReturnSales(ProductItem productItem, PaymentReceipt paymentReceipt);


  @Mapping(target = "id", ignore = true)
  @Mapping(target = "created", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "lastUpdated", ignore = true)
  @Mapping(target = "lastUpdatedBy", ignore = true)
  @Mapping(target = "returnDate", ignore = true)
  @Mapping(target = "soldPrice", source = "productItem.price", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "originalPrice", source = "productItem.originalPrice", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "wholesalePrice", source = "productItem.wholesalePrice", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "total", source = "productItem.total", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "paymentReceipt", source = "paymentReceipt")
  void updateToSale(@MappingTarget Sales sale, ProductItem productItem, PaymentReceipt paymentReceipt);

  @Named("doubleToBigDecimal")
  static BigDecimal mapDoubleToBigDecimal(double value) {
    return BigDecimal.valueOf(value);
  }
}
