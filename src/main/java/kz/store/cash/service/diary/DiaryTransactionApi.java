package kz.store.cash.service.diary;

import java.net.URI;
import kz.store.cash.model.diarydebt.DiaryTransaction;
import kz.store.cash.model.entity.AppSetting;
import kz.store.cash.model.enums.DiaryOperationType;
import kz.store.cash.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class DiaryTransactionApi { // NEW

  @Qualifier("hydraClient")
  private final RestClient hydraClient;

  private final DebtDiaryExecutor exec;
  private final DiaryDebtAuthService auth;
  private final AppSettingService appSettingService;

  private URI baseUri() {
    String base = getAppSetting().getDebtDiaryBaseAddress();
    if (base == null || base.isBlank()) {
      throw new IllegalStateException("Не задан base адрес книги задолженности");
    }
    return URI.create(base);
  }

  private URI transactionsUri() {
    return baseUri().resolve("/api/transactions");
  }

  private AppSetting getAppSetting() {
    return appSettingService.getSingleton()
        .orElseThrow(() -> new Error("Отсутствуют настройки по кассовому приложению"));
  }

  /**
   * Создаёт транзакцию «Реализация» (type=/api/types/2). Возвращает HTTP статус для явной проверки
   * 201.
   */
  public ResponseEntity<Void> createSale(DiaryTransaction diaryTransaction,
      DiaryOperationType diaryOperationType) { // NEW
    return exec.call(() -> {
      auth.ensureToken();
      String comment = formatComment(diaryOperationType);
      var body = diaryTransaction.modifyTypeAndComment(diaryOperationType, comment);
      return hydraClient.post()
          .uri(transactionsUri())
          .body(body)
          .retrieve()
          .toBodilessEntity();
    });
  }

  private String formatComment(DiaryOperationType diaryOperationType) {
    return diaryOperationType.getDisplayName() + " записан с кассы по логину: "
        + getAppSetting().getDebtDiaryLogin();
  }

}