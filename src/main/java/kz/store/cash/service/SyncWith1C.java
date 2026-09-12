package kz.store.cash.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import kz.store.cash.fx.component.UiNotificationService;
import kz.store.cash.handler.BusinessException;
import kz.store.cash.model.CategoryDto;
import kz.store.cash.model.ProductDto;
import kz.store.cash.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Получает данные из 1С по HTTP и передаёт их на запись в {@link OneCSyncWriter}. HTTP-вызов
 * намеренно НЕ обёрнут в @Transactional: пока идёт сетевой запрос к 1С, соединение из пула БД не
 * должно простаивать занятым. Транзакция открывается только внутри {@link OneCSyncWriter}, уже
 * после того как ответ от 1С получен.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncWith1C {

  @Qualifier("oneCClient")
  private final RestClient restClient;
  private final UiNotificationService uiNotificationService;
  private final AppSettingService appSettingService;
  private final OneCSyncWriter writer;

  private static final ParameterizedTypeReference<List<CategoryDto>> CATEGORY_LIST_TYPE =
      new ParameterizedTypeReference<>() {
      };
  private static final ParameterizedTypeReference<List<ProductDto>> PRODUCT_LIST_TYPE =
      new ParameterizedTypeReference<>() {
      };

  public void syncCategory() {
    var categoryDtoList = Optional.ofNullable(getCategoryDto()).orElseGet(List::of);
    if (categoryDtoList.isEmpty()) {
      throw new BusinessException("Категории из 1С не получены или пусты");
    }
    var counts = writer.applyCategorySync(categoryDtoList);
    log.info("Category sync: created={}, updated={}, categoryDto={}",
        counts.created(), counts.updated(), categoryDtoList.size());
    uiNotificationService.showInfo("Категории за синхронизированы");
  }

  public void syncProduct() {
    var productDtoList = Optional.ofNullable(getProductDto()).orElseGet(List::of);
    if (productDtoList.isEmpty()) {
      throw new BusinessException("Продукты из 1С не получены или пусты");
    }
    var counts = writer.applyProductSync(productDtoList);
    log.info("Product sync: created={}, updated={}, skippedNoCategory={}, totalInPayload={}",
        counts.created(), counts.updated(), counts.skippedNoCategory(), productDtoList.size());
    uiNotificationService.showInfo("Продукты за синхронизированы");
  }

  public void syncAll1C() {
    syncCategory();
    syncProduct();
  }

  private static String nullToEmpty(String s) {
    return StringUtils.nullToEmpty(s);
  }

  private <T> List<T> fetchList(String endpointKey, ParameterizedTypeReference<List<T>> type) {
    var appSetting = appSettingService.getSingleton().orElse(null);
    if (appSetting == null || nullToEmpty(appSetting.getCategoryWebAddress()).isEmpty()
        || nullToEmpty(appSetting.getProductWebAddress()).isEmpty()) {
      throw new BusinessException(
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
}
