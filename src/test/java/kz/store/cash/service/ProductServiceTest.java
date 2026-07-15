package kz.store.cash.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import kz.store.cash.config.ProductProperties;
import kz.store.cash.fx.model.ProductItem;
import kz.store.cash.mapper.ProductMapper;
import kz.store.cash.model.entity.Product;
import kz.store.cash.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
  @Mock ProductRepository repository;
  @Mock ProductMapper mapper;
  @Mock ProductProperties properties;
  @InjectMocks ProductService service;

  @Test void findsProductByBarcode() {
    Product product = new Product();
    ProductItem item = new ProductItem("123", "Milk", 500, 450);
    when(repository.findFirstByBarcode("123")).thenReturn(Optional.of(product));
    when(mapper.toProductItem(product)).thenReturn(item);

    assertThat(service.findByBarcode("123")).isSameAs(item);
  }

  @Test void returnsNullWhenBarcodeIsUnknown() {
    when(repository.findFirstByBarcode("missing")).thenReturn(Optional.empty());
    assertThat(service.findByBarcode("missing")).isNull();
    verifyNoInteractions(mapper);
  }

  @Test void searchesByBarcodePrefixAndNameFragment() {
    Product product = new Product();
    ProductItem item = new ProductItem("123", "Milk", 500, 450);
    when(repository.findByBarcodeLikeOrProductNameLikeIgnoreCase("mil%", "%mil%"))
        .thenReturn(List.of(product));
    when(mapper.toProductItem(product)).thenReturn(item);

    assertThat(service.searchByPartialBarcode("mil")).containsExactly(item);
  }

  @Test void updatesPositiveRetailPrice() {
    Product product = new Product();
    when(repository.findFirstByBarcode("123")).thenReturn(Optional.of(product));

    service.updateRetailPrice("123", 125.50);

    assertThat(product.getOriginalPrice()).isEqualByComparingTo(new BigDecimal("125.5"));
    verify(repository).save(product);
  }

  @Test void ignoresNonPositiveRetailPrice() {
    service.updateRetailPrice("123", 0);
    verifyNoInteractions(repository);
  }

  @Test void findsUniversalProductUsingConfiguredBarcode() {
    Product product = new Product();
    ProductItem item = new ProductItem("universal", "Product", 700, 700);
    when(properties.universalProductBarcode()).thenReturn("universal");
    when(repository.findFirstByBarcode("universal")).thenReturn(Optional.of(product));
    when(mapper.universlProductToProductItem(product, 700)).thenReturn(item);

    assertThat(service.findUniversalProduct(700)).isSameAs(item);
  }
}
