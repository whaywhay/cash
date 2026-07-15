package kz.store.cash.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import kz.store.cash.model.entity.Product;
import kz.store.cash.model.entity.QuickProduct;
import kz.store.cash.repository.QuickProductRepository;
import org.junit.jupiter.api.Test;

class QuickProductServiceTest {
  private final QuickProductRepository repository = mock(QuickProductRepository.class);
  private final ProductService products = mock(ProductService.class);
  private final QuickProductService service = new QuickProductService(repository, products);

  @Test void toggleRemovesExistingQuickProduct() {
    QuickProduct quick = new QuickProduct();
    when(repository.findByBarcode("1")).thenReturn(Optional.of(quick));
    assertThat(service.toggle("1")).isFalse();
    verify(repository).delete(quick);
  }

  @Test void toggleAddsNewQuickProduct() {
    when(repository.findByBarcode("1")).thenReturn(Optional.empty());
    assertThat(service.toggle("1")).isTrue();
    verify(repository).save(argThat(q -> q.getBarcode().equals("1") && q.getSortedDate() != null));
  }

  @Test void listProductsPreservesQuickProductOrder() {
    QuickProduct q2 = quick("2"), q1 = quick("1");
    Product p1 = product("1"), p2 = product("2");
    when(repository.findAllByOrderBySortedDateDesc()).thenReturn(List.of(q2, q1));
    when(products.findByBarcodeIn(argThat(s -> s.containsAll(List.of("1", "2")))))
        .thenReturn(List.of(p1, p2));
    assertThat(service.listProducts()).containsExactly(p2, p1);
  }

  @Test void emptyQuickListDoesNotQueryProducts() {
    when(repository.findAllByOrderBySortedDateDesc()).thenReturn(List.of());
    assertThat(service.listProducts()).isEmpty();
    verifyNoInteractions(products);
  }

  @Test void isQuickDelegates() {
    when(repository.existsByBarcode("1")).thenReturn(true);
    assertThat(service.isQuick("1")).isTrue();
  }
  private QuickProduct quick(String barcode) { var q = new QuickProduct(); q.setBarcode(barcode); return q; }
  private Product product(String barcode) { var p = new Product(); p.setBarcode(barcode); return p; }
}
