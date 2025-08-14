package kz.store.cash.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import java.math.BigDecimal;
import kz.store.cash.model.entity.Product;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.fx.model.SalesWithProductName;
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
  static double mapBigDecimalToDouble(BigDecimal value) {
    return value != null ? value.doubleValue() : 0.0;
  }

  @Mapping(target = "productName", source = "product.productName")
  @Mapping(target = "barcode", source = "product.barcode")
  @Mapping(target = "originalPrice", source = "price")
  @Mapping(target = "wholesalePrice", source = "price")
  ProductItem universlProductToProductItem(Product product, double price);

  // -------- SalesWithProductName → ProductItem ----------
  @Mapping(target = "productName", source = "productName")
  @Mapping(target = "barcode", source = "barcode")
  @Mapping(target = "originalPrice", source = "originalPrice", qualifiedByName = "bigDecimalToDouble")
  @Mapping(target = "wholesalePrice", source = "wholesalePrice", qualifiedByName = "bigDecimalToDouble")
  @Mapping(target = "price", source = "soldPrice", qualifiedByName = "bigDecimalToDouble")
  @Mapping(target = "quantity", source = "quantity")
  @Mapping(target = "salesId", source = "id")
  ProductItem toProductItem(SalesWithProductName sale);

}
