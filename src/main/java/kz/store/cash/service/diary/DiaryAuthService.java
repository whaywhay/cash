package kz.store.cash.service.diary;

import kz.store.cash.handler.ExternalResponseError;
import kz.store.cash.handler.ValidationException;
import kz.store.cash.model.DiaryLoginReq;
import kz.store.cash.model.DiaryTokenRes;
import kz.store.cash.model.entity.AppSetting;
import kz.store.cash.service.AppSettingService;
import kz.store.cash.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class DiaryAuthService {

  @Qualifier("rawHydraClient")
  private final RestClient rawHydraClient;
  private final TokenHolder tokenHolder;
  private final AppSettingService appSettingService;

  public void logout() {
    tokenHolder.clear();
  }

  public void login() {
    var s = getAppSettingAndCheckDiaryData();
    authenticate(s.getDebtDiaryBaseAddress(), s.getDebtDiaryLogin(), s.getDebtDiaryPassword());
  }

  public void authenticate(String baseAddress, String login, String password) {
    String base = StringUtils.nullToEmpty(baseAddress).trim();
    System.out.println("Data for authenticate : " + base + "/token : " + login + " : " + password);
    var res = rawHydraClient.post()
        .uri(baseAddress + "/token")
        .body(new DiaryLoginReq(login, password))
        .retrieve()
        .body(DiaryTokenRes.class);

    System.out.println("DiaryTokenRes.class: " + res);
    if (res == null || res.token() == null || res.token().isBlank()) {
      throw new ExternalResponseError("Token missing in /token response");
    }
    tokenHolder.set(res.token());
  }

  public void ensureToken() {
    if (!tokenHolder.isPresent()) {
      login();
    }
  }

  private AppSetting getAppSettingAndCheckDiaryData() {
    var appSetting = appSettingService.getSingleton().orElse(null);
    if (appSetting == null) {
      throw new ValidationException("Отсутствует данные в настройках");
    }
    if (isBlank(appSetting.getDebtDiaryBaseAddress())
        || isBlank(appSetting.getDebtDiaryLogin())
        || isBlank(appSetting.getDebtDiaryPassword())) {
      throw new ValidationException("Отсутствуют данные в настройках по Книге задолженности");
    }
    return appSetting;
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}