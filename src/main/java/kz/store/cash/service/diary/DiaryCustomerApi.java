package kz.store.cash.service.diary;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import kz.store.cash.model.DiaryCustomer;
import kz.store.cash.model.HydraCollection;
import kz.store.cash.model.PageResult;
import kz.store.cash.model.entity.AppSetting;
import kz.store.cash.service.AppSettingService;
import kz.store.cash.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class DiaryCustomerApi {

  @Qualifier("hydraClient")
  private final RestClient hydraClient;
  private final DebtDiaryExecutor exec;
  private final DiaryAuthService auth;
  private final AppSettingService appSettingService;

  private static final ParameterizedTypeReference<HydraCollection<DiaryCustomer>> TYPE =
      new ParameterizedTypeReference<>() {
      };

  /* ===================== ПУБЛИЧНЫЕ API ===================== */

  public PageResult<DiaryCustomer> getPage(int page, String search, String order) {
    return exec.call(() -> {
      auth.ensureToken();
      URI uri = buildCustomersUri(page, search, order);
      var hc = fetchCustomers(uri, "GET /api/customers");
      return toPageResult(hc, page);
    });
  }

  /* ===================== ВНУТРЕННЕЕ ===================== */

  private URI baseUri() {
    String base = appSettingService.getSingleton()
        .map(AppSetting::getDebtDiaryBaseAddress)
        .orElseThrow(() -> new IllegalStateException("Не задан base адрес книги задолженности"));
    return URI.create(base);
  }

  private URI buildCustomersUri(int page, String search, String order) {
    var b = UriComponentsBuilder.fromUri(baseUri())
        .path("/api/customers")
        .queryParam("page", Math.max(1, page));

    Optional.ofNullable(trimToNull(search))
        .ifPresent(s -> b.queryParam("search", s));

    Optional.ofNullable(trimToNull(order))
        .map(this::normalizeOrder)
        .ifPresent(o -> b.queryParam("order[" + o.field + "]", o.dir));

    return b.build().encode(StandardCharsets.UTF_8).toUri();
  }

  private OrderSpec normalizeOrder(String order) {
    String field = order.replaceFirst("^-", "");
    String dir = order.startsWith("-") ? "desc" : "asc";
    String mapped = switch (field) {
      case "place" -> "place";
      case "total" -> "total";
      case "last_transaction", "lastTransactionAt" -> "last_transaction";
      default -> "name";
    };
    return new OrderSpec(mapped, dir);
  }

  private record OrderSpec(String field, String dir) {

  }

  /**
   * Централизованный фетчер: гарантирует non-null body.
   */
  private HydraCollection<DiaryCustomer> fetchCustomers(URI uri, String context) {
    var body = hydraClient.get().uri(uri).retrieve().body(TYPE);
    if (body == null) {
      throw new IllegalStateException("Пустой ответ от сервиса Книги задолженности: " + context);
    }
    return body;
  }

  private <T> PageResult<T> toPageResult(HydraCollection<T> hc, int fallbackPage) {
    int totalItems = hc.totalItems != null ? hc.totalItems : 0;
    int lastPage = extractPage(hc.view != null ? hc.view.last : null);
    int selfPage = extractPage(hc.view != null ? hc.view.self : null);

    int current = selfPage > 0 ? selfPage : Math.max(1, fallbackPage);
    int totalPages = lastPage > 0 ? lastPage : current;

    // защищаемся от null-списка
    List<T> items = hc.member != null ? hc.member : List.of();

    return new PageResult<>(
        items,
        current,
        totalPages,
        totalItems,
        hc.view != null ? hc.view.next : null,
        hc.view != null ? hc.view.previous : null
    );
  }

  private static final Pattern PAGE_RE = Pattern.compile("[?&]page=(\\d+)");

  private static int extractPage(String url) {
    if (url == null) {
      return 0;
    }
    var m = PAGE_RE.matcher(url);
    return m.find() ? Integer.parseInt(m.group(1)) : 0;
  }

  private static String trimToNull(String s) {
    return StringUtils.trimSafely(s);
  }
}