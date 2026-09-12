package kz.store.cash.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import kz.store.cash.mapper.CategoryMapper;
import kz.store.cash.mapper.ProductMapper;
import kz.store.cash.model.CategoryDto;
import kz.store.cash.model.ProductDto;
import kz.store.cash.model.entity.Category;
import kz.store.cash.model.entity.Product;
import kz.store.cash.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Отделяет запись результатов синхронизации 1С в БД от HTTP-получения данных в {@link SyncWith1C}.
 * Должен оставаться отдельным Spring-бином: @Transactional работает через прокси, а
 * self-invocation (вызов метода того же класса через this) прокси не проходит — поэтому метод
 * обновления существующих записей (categoryMapper.updateToCategory/productMapper.updateToProduct
 * мутируют сущности без явного save()) должен вызываться именно как метод ДРУГОГО бина, а не
 * как приватный метод внутри SyncWith1C.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class OneCSyncWriter {

  private final CategoryService categoryService;
  private final ProductService productService;
  private final CategoryMapper categoryMapper;
  private final ProductMapper productMapper;

  record SyncCounts(int created, long updated, long skippedNoCategory) {}

  @Transactional
  SyncCounts applyCategorySync(List<CategoryDto> categoryDtoList) {
    Map<String, CategoryDto> categoryDtosByCodeMap = categoryDtoList.stream()
        .filter(catDto -> catDto.categoryId() != null && !catDto.categoryId().isEmpty())
        .map(d -> Map.entry(d.categoryId(), d))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
            (a, b) -> a, LinkedHashMap::new));

    Map<String, Category> existingCategoryByCodeMap = categoryService.findAllByCategoryCodeIn(
            categoryDtosByCodeMap.keySet())
        .stream()
        .collect(Collectors.toMap(Category::getCategoryCode, Function.identity()));

    long updatedCount = 0;
    for (var e : categoryDtosByCodeMap.entrySet()) {
      Category existing = existingCategoryByCodeMap.get(e.getKey());
      if (existing != null) {
        categoryMapper.updateToCategory(existing, e.getValue());
        updatedCount++;
      }
    }

    List<Category> newCategoryList = categoryDtosByCodeMap.entrySet().stream()
        .filter(e -> !existingCategoryByCodeMap.containsKey(e.getKey()))
        .map(e -> categoryMapper.dtoToCategory(e.getValue()))
        .toList();

    if (!newCategoryList.isEmpty()) {
      categoryService.saveAll(newCategoryList);
    }
    return new SyncCounts(newCategoryList.size(), updatedCount, 0);
  }

  @Transactional
  SyncCounts applyProductSync(List<ProductDto> productDtoList) {
    Map<String, ProductDto> productDtosByBarcodeMap = new LinkedHashMap<>();
    for (var productDto : productDtoList) {
      var barcode = productDto.barcode();
      if (barcode == null || barcode.isBlank()) {
        continue;
      }
      productDtosByBarcodeMap.putIfAbsent(barcode.strip(), productDto);
    }

    Set<String> categoryCodesInProductDtos = productDtosByBarcodeMap.values().stream()
        .map(ProductDto::categoryRefId)
        .map(StringUtils::trimSafely)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    Map<String, Category> categoriesByCode = categoryCodesInProductDtos.isEmpty()
        ? Map.of() : categoryService.findAllByCategoryCodeIn(categoryCodesInProductDtos).stream()
        .collect(Collectors.toMap(Category::getCategoryCode, Function.identity()));
    Map<String, Product> existingProductsByBarcode = productDtosByBarcodeMap.isEmpty()
        ? Map.of() : productService.findByBarcodeIn(productDtosByBarcodeMap.keySet()).stream()
        .collect(Collectors.toMap(Product::getBarcode, Function.identity()));

    long skippedNoCategoryCount = 0;
    for (var e : productDtosByBarcodeMap.entrySet()) {
      String categoryCode = StringUtils.trimSafely(e.getValue().categoryRefId());
      if (categoryCode != null && !categoriesByCode.containsKey(categoryCode)) {
        log.warn("Пропуск товара barcode={} — категория {} не найдена", e.getKey(), categoryCode);
        skippedNoCategoryCount++;
      }
    }

    long updatedCount = 0;
    for (var e : productDtosByBarcodeMap.entrySet()) {
      if (!existingProductsByBarcode.containsKey(e.getKey())) {
        continue;
      }
      String categoryCode = StringUtils.trimSafely(e.getValue().categoryRefId());
      if (categoryCode != null && !categoriesByCode.containsKey(categoryCode)) {
        continue;
      }
      var productDto = e.getValue();
      var category = categoriesByCode.get(categoryCode);
      var product = existingProductsByBarcode.get(e.getKey());
      productMapper.updateToProduct(product, productDto, category);
      if (product.getOriginalPrice() == null) {
        product.setOriginalPrice(BigDecimal.ZERO);
      }
      updatedCount++;
    }

    List<Product> newProductList = productDtosByBarcodeMap.entrySet().stream()
        .filter(e -> !existingProductsByBarcode.containsKey(e.getKey()))
        .filter(e -> {
          String catCode = StringUtils.trimSafely(e.getValue().categoryRefId());
          return catCode == null || categoriesByCode.containsKey(catCode);
        })
        .map(e -> {
          var dto = e.getValue();
          var cat = categoriesByCode.get(StringUtils.trimSafely(dto.categoryRefId()));
          var p = productMapper.productDtoToProduct(dto, cat);
          if (p.getOriginalPrice() == null) {
            p.setOriginalPrice(BigDecimal.ZERO);
          }
          return p;
        })
        .toList();

    if (!newProductList.isEmpty()) {
      productService.saveAll(newProductList);
    }
    return new SyncCounts(newProductList.size(), updatedCount, skippedNoCategoryCount);
  }
}
