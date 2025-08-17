package kz.store.cash.service;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import kz.store.cash.config.ApiUrlProperties;
import kz.store.cash.mapper.CategoryMapper;
import kz.store.cash.mapper.ProductMapper;
import kz.store.cash.model.CategoryDto;
import kz.store.cash.model.ProductDto;
import kz.store.cash.model.entity.Category;
import kz.store.cash.model.entity.Product;
import kz.store.cash.repository.CategoryRepository;
import kz.store.cash.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryAndProduct1C {

  private final RestClient restClient;
  private final ApiUrlProperties apiUrlProperties;
  private final CategoryRepository categoryRepository;
  private final ProductRepository productRepository;
  private final CategoryMapper categoryMapper;
  private final ProductMapper productMapper;

  private static final ParameterizedTypeReference<List<CategoryDto>> CATEGORY_LIST_TYPE =
      new ParameterizedTypeReference<>() {
      };
  private static final ParameterizedTypeReference<List<ProductDto>> PRODUCT_LIST_TYPE =
      new ParameterizedTypeReference<>() {
      };


  @Transactional
  public void syncCategory() {
    var categoryDtoList = Optional.ofNullable(getCategoryDto()).orElseGet(List::of);
    if (categoryDtoList.isEmpty()) {
      log.warn("Категории из 1С не получены или пусты");
      return;
    }
    //Собираем в Map по categoryId и CategoryDto и пропускаем через фильтр
    Map<String, CategoryDto> categoryDtosByCodeMap = categoryDtoList.stream()
        .filter(catDto -> catDto.categoryId() != null && !catDto.categoryId().isEmpty())
        .map(d -> Map.entry(d.categoryId(), d))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
            (a, b) -> a, LinkedHashMap::new));
    // Вытаскиваем с таблицы category все совпадающие с CategoryDto.categoryId одним запросом
    Map<String, Category> existingCategoryByCodeMap = categoryRepository.findAllByCategoryCodeIn(
            categoryDtosByCodeMap.keySet())
        .stream()
        .collect(Collectors.toMap(Category::getCategoryCode, Function.identity()));

    // Обновляем category таблицу если соответствует записи по CategoryDto.categoryId = Category.categoryCode
    long updatedCount = categoryDtosByCodeMap.entrySet().stream()
        .filter(e -> existingCategoryByCodeMap.containsKey(e.getKey()))
        .peek(e -> categoryMapper.updateToCategory(existingCategoryByCodeMap.get(e.getKey()),
            e.getValue()))
        .count();

    // Вставки
    List<Category> newCategoryList = categoryDtosByCodeMap.entrySet().stream()
        .filter(e -> !existingCategoryByCodeMap.containsKey(e.getKey()))
        .map(e -> categoryMapper.dtoToCategory(e.getValue()))
        .toList();

    if (!newCategoryList.isEmpty()) {
      categoryRepository.saveAll(newCategoryList);
    }

    log.info("Category sync: created={}, updated={}, categoryDto={}",
        newCategoryList.size(), updatedCount, categoryDtoList.size());
  }

  @Transactional
  public void syncProduct() {
    var productDtoList = Optional.ofNullable(getProductDto()).orElseGet(List::of);
    if (productDtoList.isEmpty()) {
      log.warn("Продукты из 1С не получены или пусты");
      return;
    }
    //Собираем в Map по barcode и ProductDto и пропускаем через фильтр
    Map<String, ProductDto> productDtosByBarcodeMap = productDtoList.stream()
        .filter(prodDto -> prodDto.barcode() != null && !prodDto.barcode().isEmpty())
        .map(d -> Map.entry(d.barcode(), d))
        .collect(Collectors.toMap(
            Map.Entry::getKey, Map.Entry::getValue,
            (a, b) -> a, LinkedHashMap::new));

    // Собираем в Set<String> нужные category_code из List<productDto>
    Set<String> categoryCodesInProductDtos = productDtosByBarcodeMap.values().stream()
        .map(ProductDto::categoryRefId)
        .map(CategoryAndProduct1C::trimToNull)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    // Вытаскиваем с таблицы category все совпадающие
    // с ProductDto.categoryRefId(Set<String> categoryCodesInProductDtos) одним запросом
    Map<String, Category> categoriesByCode = categoryCodesInProductDtos.isEmpty()
        ? Map.of() : categoryRepository.findAllByCategoryCodeIn(categoryCodesInProductDtos).stream()
        .collect(Collectors.toMap(Category::getCategoryCode, Function.identity()));
    // Вытаскиваем с таблицы product все совпадающие по ProductDto.barcode одним запросом
    Map<String, Product> existingProductsByBarcode = productDtosByBarcodeMap.isEmpty()
        ? Map.of() : productRepository.findByBarcodeIn(productDtosByBarcodeMap.keySet()).stream()
        .collect(Collectors.toMap(Product::getBarcode, Function.identity()));

    // Сколько пропущено из-за отсутствующей категории (если categoryCode указан, а category не найдена)
    long skippedNoCategoryCount = productDtosByBarcodeMap.entrySet().stream()
        .filter(e -> {
          String categoryCode = trimToNull(e.getValue().categoryRefId());
          return categoryCode != null && !categoriesByCode.containsKey(categoryCode);
        })
        .peek(e -> log.warn("Пропуск товара barcode={} — категория {} не найдена",
            e.getKey(), trimToNull(e.getValue().categoryRefId())))
        .count();

    // Обновления (есть продукт в таблице product, и нет конфликта с категорией category)
    long updatedCount = productDtosByBarcodeMap.entrySet().stream()
        .filter(e -> existingProductsByBarcode.containsKey(e.getKey()))
        .filter(e -> {
          String categoryCode = trimToNull(e.getValue().categoryRefId());
          return categoryCode == null || categoriesByCode.containsKey(categoryCode);
        })
        .peek(e -> {
          var productDto = e.getValue();
          var category = categoriesByCode.get(trimToNull(productDto.categoryRefId()));
          var product = existingProductsByBarcode.get(e.getKey());
          productMapper.updateToProduct(product, productDto, category);
          if (product.getOriginalPrice() == null) {
            product.setOriginalPrice(BigDecimal.ZERO);
          }
        })
        .count();

    // Вставки (нет продукта в таблице product, и нет конфликта с категорией category)
    List<Product> newProductList = productDtosByBarcodeMap.entrySet().stream()
        .filter(e -> !existingProductsByBarcode.containsKey(e.getKey()))
        .filter(e -> {
          String catCode = trimToNull(e.getValue().categoryRefId());
          return catCode == null || categoriesByCode.containsKey(catCode);
        })
        .map(e -> {
          var dto = e.getValue();
          var cat = categoriesByCode.get(trimToNull(dto.categoryRefId()));
          var p = productMapper.productDtoToProduct(dto, cat);
          if (p.getOriginalPrice() == null) {
            p.setOriginalPrice(BigDecimal.ZERO);
          }
          return p;
        })
        .toList();

    if (!newProductList.isEmpty()) {
      productRepository.saveAll(newProductList);
    }

    log.info("Product sync: created={}, updated={}, skippedNoCategory={}, totalInPayload={}",
        newProductList.size(), updatedCount, skippedNoCategoryCount, productDtoList.size());
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  private static String trimSafely(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private static String trimToNull(String s) {
    return trimSafely(s);
  }

  private <T> List<T> fetchList(String endpointKey, ParameterizedTypeReference<List<T>> type) {
    var apiProperty = apiUrlProperties.urlBasic().get(endpointKey);
    if (apiProperty == null) {
      throw new IllegalStateException("Нет настроек endpoint: " + endpointKey);
    }

    URI uri = apiProperty.baseUrl();
    String auth = "Basic " + Base64.getEncoder()
        .encodeToString(
            (nullToEmpty(apiProperty.username()) + ":" + nullToEmpty(apiProperty.password()))
                .getBytes(StandardCharsets.UTF_8));

    return restClient.get()
        .uri(uri)
        .header("Authorization", auth)
        .retrieve()
        .body(type);
  }

  private List<CategoryDto> getCategoryDto() {
    return fetchList("category1c", CATEGORY_LIST_TYPE);
  }

  private List<ProductDto> getProductDto() {
    return fetchList("product1c", PRODUCT_LIST_TYPE);
  }

  @Transactional
  public void syncAll1C() {
    syncCategory();
    syncProduct();
  }
}
