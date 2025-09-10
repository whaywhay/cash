package kz.store.cash.service;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import kz.store.cash.fx.component.UiNotificationService;
import kz.store.cash.handler.BusinessException;
import kz.store.cash.mapper.CategoryMapper;
import kz.store.cash.mapper.ProductMapper;
import kz.store.cash.model.CategoryDto;
import kz.store.cash.model.ProductDto;
import kz.store.cash.model.entity.Category;
import kz.store.cash.model.entity.Product;
import kz.store.cash.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncWith1C {

  private final RestClient restClient;
  private final CategoryService categoryService;
  private final ProductService productService;
  private final CategoryMapper categoryMapper;
  private final ProductMapper productMapper;
  private final UiNotificationService uiNotificationService;
  private final AppSettingService appSettingService;

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
      throw new BusinessException("Категории из 1С не получены или пусты");
    }
    //Собираем в Map по categoryId и CategoryDto и пропускаем через фильтр
    Map<String, CategoryDto> categoryDtosByCodeMap = categoryDtoList.stream()
        .filter(catDto -> catDto.categoryId() != null && !catDto.categoryId().isEmpty())
        .map(d -> Map.entry(d.categoryId(), d))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
            (a, b) -> a, LinkedHashMap::new));
    // Вытаскиваем с таблицы category все совпадающие с CategoryDto.categoryId одним запросом
    Map<String, Category> existingCategoryByCodeMap = categoryService.findAllByCategoryCodeIn(
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
      categoryService.saveAll(newCategoryList);
    }
    String info = String.format("Category sync: created=%d, updated=%d, categoryDto=%d",
        newCategoryList.size(), updatedCount, categoryDtoList.size());
    log.info(info);
    uiNotificationService.showInfo("Категории за синхронизированы");
  }

  @Transactional
  public void syncProduct() {
    var productDtoList = Optional.ofNullable(getProductDto()).orElseGet(List::of);
    if (productDtoList.isEmpty()) {
      throw new BusinessException("Продукты из 1С не получены или пусты");
    }
    //Собираем в Map productDtosByBarcodeMap по barcode и ProductDto и пропускаем через фильтр
    Map<String, ProductDto> productDtosByBarcodeMap = new LinkedHashMap<>();
    for (var productDto : productDtoList) {
      var barcode = productDto.barcode();
      if (barcode == null || barcode.isBlank()) {
        continue;
      }
      productDtosByBarcodeMap.putIfAbsent(barcode.strip(), productDto);
    }
    // Собираем в Set<String> нужные category_code из List<productDto>
    Set<String> categoryCodesInProductDtos = productDtosByBarcodeMap.values().stream()
        .map(ProductDto::categoryRefId)
        .map(SyncWith1C::trimToNull)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    // Вытаскиваем с таблицы category все совпадающие
    // с ProductDto.categoryRefId(Set<String> categoryCodesInProductDtos) одним запросом
    Map<String, Category> categoriesByCode = categoryCodesInProductDtos.isEmpty()
        ? Map.of() : categoryService.findAllByCategoryCodeIn(categoryCodesInProductDtos).stream()
        .collect(Collectors.toMap(Category::getCategoryCode, Function.identity()));
    // Вытаскиваем с таблицы product все совпадающие по ProductDto.barcode одним запросом
    Map<String, Product> existingProductsByBarcode = productDtosByBarcodeMap.isEmpty()
        ? Map.of() : productService.findByBarcodeIn(productDtosByBarcodeMap.keySet()).stream()
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
      productService.saveAll(newProductList);
    }

    String info = String.format(
        "Product sync: created=%d, updated=%d, skippedNoCategory=%d, totalInPayload=%d",
        newProductList.size(), updatedCount, skippedNoCategoryCount, productDtoList.size());
    log.info(info);
    uiNotificationService.showInfo("Продукты за синхронизированы");
  }

  private static String nullToEmpty(String s) {
    return StringUtils.nullToEmpty(s);
  }

  private static String trimToNull(String s) {
    return StringUtils.trimSafely(s);
  }

  private <T> List<T> fetchList(String endpointKey, ParameterizedTypeReference<List<T>> type) {
    var appSetting = appSettingService.getSingleton().orElse(null);
    if (appSetting == null || nullToEmpty(appSetting.getCategoryWebAddress()).isEmpty()
        || nullToEmpty(appSetting.getProductWebAddress()).isEmpty()) {
      throw new IllegalStateException(
          "Нет настроек по организации или нет endpoint адресов: 1C категории и продукты");
    }
    URI uri = URI.create(appSetting.getCategoryWebAddress());
    if (endpointKey.equals("category1c")) {
      uri = URI.create(appSetting.getCategoryWebAddress());
    } else if (endpointKey.equals("product1c")) {
      uri = URI.create(appSetting.getProductWebAddress());
    }
    String auth = "Basic " + Base64.getEncoder()
        .encodeToString(
            (nullToEmpty(appSetting.getWebLogin()) + ":" + nullToEmpty(appSetting.getWebPassword()))
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
