package kz.store.cash.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import kz.store.cash.mapper.CategoryMapper;
import kz.store.cash.mapper.ProductMapper;
import kz.store.cash.model.CategoryDto;
import kz.store.cash.model.ProductDto;
import kz.store.cash.model.entity.Category;
import kz.store.cash.model.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;

class OneCSyncWriterTest {

  CategoryService categories = mock(CategoryService.class);
  ProductService products = mock(ProductService.class);
  CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);
  ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);
  OneCSyncWriter writer;

  @BeforeEach void setUp() {
    writer = new OneCSyncWriter(categories, products, categoryMapper, productMapper);
  }

  // ---------- категории ----------

  @Test void createsOnlyCategoriesThatDoNotExistYet() {
    when(categories.findAllByCategoryCodeIn(Set.of("A", "B"))).thenReturn(List.of(category("A", "old")));

    var counts = writer.applyCategorySync(List.of(cat("A", "new A", null), cat("B", "new B", "A")));

    assertThat(counts.created()).isEqualTo(1);
    assertThat(counts.updated()).isEqualTo(1);
    assertThat(savedCategories()).singleElement().satisfies(c -> {
      assertThat(c.getCategoryCode()).isEqualTo("B");
      assertThat(c.getCategoryName()).isEqualTo("new B");
    });
  }

  @Test void updatesExistingCategoryInPlaceWithoutSaving() {
    Category existing = category("A", "old");
    when(categories.findAllByCategoryCodeIn(Set.of("A"))).thenReturn(List.of(existing));

    var counts = writer.applyCategorySync(List.of(cat("A", "renamed", null)));

    assertThat(existing.getCategoryName()).isEqualTo("renamed");
    assertThat(counts.created()).isZero();
    assertThat(counts.updated()).isEqualTo(1);
    verify(categories, never()).saveAll(any());
  }

  @Test void ignoresCategoriesWithoutCodeAndDuplicatesKeepFirst() {
    when(categories.findAllByCategoryCodeIn(Set.of("A"))).thenReturn(List.of());

    var counts = writer.applyCategorySync(List.of(
        cat(null, "no code", null), cat("", "empty code", null),
        cat("A", "first", null), cat("A", "second", null)));

    assertThat(counts.created()).isEqualTo(1);
    assertThat(savedCategories()).singleElement().extracting(Category::getCategoryName).isEqualTo("first");
  }

  @Test void categoryPayloadWithoutValidCodesWritesNothing() {
    when(categories.findAllByCategoryCodeIn(Set.of())).thenReturn(List.of());

    var counts = writer.applyCategorySync(List.of(cat(null, "x", null)));

    assertThat(counts.created()).isZero();
    assertThat(counts.updated()).isZero();
    verify(categories, never()).saveAll(any());
  }

  // ---------- товары ----------

  @Test void createsNewProductLinkedToItsCategory() {
    Category cat = category("C1", "cat");
    when(categories.findAllByCategoryCodeIn(Set.of("C1"))).thenReturn(List.of(cat));
    when(products.findByBarcodeIn(Set.of("111"))).thenReturn(List.of());

    var counts = writer.applyProductSync(List.of(product("C1", "Milk", "111", "250.00")));

    assertThat(counts.created()).isEqualTo(1);
    assertThat(counts.updated()).isZero();
    assertThat(savedProducts()).singleElement().satisfies(p -> {
      assertThat(p.getBarcode()).isEqualTo("111");
      assertThat(p.getProductName()).isEqualTo("Milk");
      assertThat(p.getCategoryRef()).isSameAs(cat);
      assertThat(p.getWholesalePrice()).isEqualByComparingTo("250.00");
    });
  }

  @Test void newProductGetsZeroOriginalPriceWhenMapperLeavesItNull() {
    when(categories.findAllByCategoryCodeIn(Set.of("C1"))).thenReturn(List.of(category("C1", "cat")));
    when(products.findByBarcodeIn(Set.of("111"))).thenReturn(List.of());

    writer.applyProductSync(List.of(product("C1", "Milk", "111", "1.00")));

    assertThat(savedProducts()).singleElement()
        .extracting(Product::getOriginalPrice).isEqualTo(BigDecimal.ZERO);
  }

  @Test void updatesExistingProductAndFillsMissingOriginalPrice() {
    Category cat = category("C1", "cat");
    Product existing = new Product(); existing.setBarcode("111"); existing.setProductName("old");
    when(categories.findAllByCategoryCodeIn(Set.of("C1"))).thenReturn(List.of(cat));
    when(products.findByBarcodeIn(Set.of("111"))).thenReturn(List.of(existing));

    var counts = writer.applyProductSync(List.of(product("C1", "Milk 2", "111", "300.00")));

    assertThat(counts.updated()).isEqualTo(1);
    assertThat(counts.created()).isZero();
    assertThat(existing.getProductName()).isEqualTo("Milk 2");
    assertThat(existing.getCategoryRef()).isSameAs(cat);
    assertThat(existing.getOriginalPrice()).isEqualTo(BigDecimal.ZERO);
    verify(products, never()).saveAll(any());
  }

  @Test void skipsProductsWhoseCategoryIsUnknownAndCountsThem() {
    Product existing = new Product(); existing.setBarcode("222"); existing.setProductName("untouched");
    when(categories.findAllByCategoryCodeIn(Set.of("GONE"))).thenReturn(List.of());
    when(products.findByBarcodeIn(Set.of("111", "222"))).thenReturn(List.of(existing));

    var counts = writer.applyProductSync(List.of(
        product("GONE", "new", "111", "1.00"), product("GONE", "changed", "222", "1.00")));

    assertThat(counts.skippedNoCategory()).isEqualTo(2);
    assertThat(counts.created()).isZero();
    assertThat(counts.updated()).isZero();
    assertThat(existing.getProductName()).isEqualTo("untouched");
    verify(products, never()).saveAll(any());
  }

  @Test void ignoresProductsWithoutBarcodeAndStripsWhitespaceAndKeepsFirstDuplicate() {
    when(categories.findAllByCategoryCodeIn(Set.of("C1"))).thenReturn(List.of(category("C1", "cat")));
    when(products.findByBarcodeIn(Set.of("111"))).thenReturn(List.of());

    var counts = writer.applyProductSync(List.of(
        product("C1", "no barcode", null, "1.00"), product("C1", "blank", "  ", "1.00"),
        product("C1", "first", " 111 ", "1.00"), product("C1", "dup", "111", "9.00")));

    assertThat(counts.created()).isEqualTo(1);
    assertThat(savedProducts()).singleElement().extracting(Product::getProductName).isEqualTo("first");
  }

  @Test void categoryCodeIsTrimmedBeforeLookup() {
    when(categories.findAllByCategoryCodeIn(Set.of("C1"))).thenReturn(List.of(category("C1", "cat")));
    when(products.findByBarcodeIn(Set.of("111"))).thenReturn(List.of());

    var counts = writer.applyProductSync(List.of(product("  C1 ", "Milk", "111", "1.00")));

    assertThat(counts.created()).isEqualTo(1);
    assertThat(counts.skippedNoCategory()).isZero();
  }

  @Test void productsWithoutCategoryAreStillSynchronized() {
    when(products.findByBarcodeIn(Set.of("111"))).thenReturn(List.of());

    var counts = writer.applyProductSync(List.of(product(null, "Loose", "111", "1.00")));

    assertThat(counts.created()).isEqualTo(1);
    assertThat(counts.skippedNoCategory()).isZero();
    assertThat(savedProducts()).singleElement().satisfies(p -> assertThat(p.getCategoryRef()).isNull());
  }

  @Test void existingProductWithoutCategoryIsUpdated() {
    Product existing = new Product(); existing.setBarcode("111"); existing.setProductName("old");
    when(products.findByBarcodeIn(Set.of("111"))).thenReturn(List.of(existing));

    var counts = writer.applyProductSync(List.of(product(null, "renamed", "111", "1.00")));

    assertThat(counts.updated()).isEqualTo(1);
    assertThat(existing.getProductName()).isEqualTo("renamed");
  }

  @Test void productPayloadWithoutBarcodesWritesNothing() {
    var counts = writer.applyProductSync(List.of(product("C1", "x", null, "1.00")));

    assertThat(counts.created()).isZero();
    assertThat(counts.updated()).isZero();
    verify(products, never()).saveAll(any());
  }

  private List<Category> savedCategories() {
    @SuppressWarnings("unchecked") ArgumentCaptor<List<Category>> c = ArgumentCaptor.forClass(List.class);
    verify(categories).saveAll(c.capture());
    return c.getValue();
  }

  private List<Product> savedProducts() {
    @SuppressWarnings("unchecked") ArgumentCaptor<java.util.Collection<Product>> c =
        ArgumentCaptor.forClass(java.util.Collection.class);
    verify(products).saveAll(c.capture());
    return List.copyOf(c.getValue());
  }

  private static CategoryDto cat(String code, String name, String parent) {
    return new CategoryDto(code, name, parent);
  }

  private static ProductDto product(String categoryRef, String name, String barcode, String wholesale) {
    return new ProductDto(categoryRef, name, barcode, new BigDecimal(wholesale));
  }

  private static Category category(String code, String name) {
    Category c = new Category(); c.setCategoryCode(code); c.setCategoryName(name);
    return c;
  }
}
