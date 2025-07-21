package kz.store.cash.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import java.math.BigDecimal;
import kz.store.cash.entity.Sales;
import kz.store.cash.fx.model.ProductItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SalesMapper {


  @Mapping(target = "soldPrice", source = "price", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "originalPrice", source = "originalPrice", qualifiedByName = "doubleToBigDecimal")
  @Mapping(target = "wholesalePrice", source = "wholesalePrice", qualifiedByName = "doubleToBigDecimal")
  Sales fromProductItemToSales(ProductItem productItem);

  @Named("doubleToBigDecimal")
  static BigDecimal mapDoubleToBigDecimal(double value) {
    return BigDecimal.valueOf(value);
  }
}
