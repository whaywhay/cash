package kz.store.cash.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import kz.store.cash.entity.Product;
import kz.store.cash.fx.model.ProductItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

  @Mapping(target = "productName", source = "productName")
  @Mapping(target = "barcode", source = "barcode")
  @Mapping(target = "originalPrice", source = "originalPrice", qualifiedByName = "bigDecimalToDouble")
  @Mapping(target = "wholesalePrice", source = "wholesalePrice", qualifiedByName = "bigDecimalToDouble")
  ProductItem toProductItem(Product product);

  @Named("bigDecimalToDouble")
  static double mapBigDecimalToDouble(java.math.BigDecimal value) {
    return value != null ? value.doubleValue() : 0.0;
  }

  @Mapping(target = "productName", source = "product.productName")
  @Mapping(target = "barcode", source = "product.barcode")
  @Mapping(target = "originalPrice", source = "price")
  @Mapping(target = "wholesalePrice", source = "price")
  ProductItem universlProductToProductItem(Product product, double price);
}
