package kz.store.cash.service.diary;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import kz.store.cash.handler.ExternalResponseError;
import kz.store.cash.handler.ValidationException;
import kz.store.cash.model.diarydebt.DiaryTokenRes;
import kz.store.cash.model.diarydebt.DiaryLoginReq;
import kz.store.cash.model.entity.AppSetting;
import kz.store.cash.service.AppSettingService;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class DiaryDebtAuthServiceTest {
  RestClient client = mock(RestClient.class);
  TokenHolder holder = new TokenHolder();
  AppSettingService settings = mock(AppSettingService.class);
  DiaryDebtAuthService service = new DiaryDebtAuthService(client, holder, settings);

  private void response(DiaryTokenRes token) {
    RestClient.RequestBodyUriSpec post = mock(RestClient.RequestBodyUriSpec.class);
    RestClient.RequestBodySpec body = mock(RestClient.RequestBodySpec.class);
    RestClient.ResponseSpec response = mock(RestClient.ResponseSpec.class);
    when(client.post()).thenReturn(post);
    when(post.uri(anyString())).thenReturn(body);
    doReturn(body).when(body).body(any(DiaryLoginReq.class));
    when(body.retrieve()).thenReturn(response);
    when(response.body(DiaryTokenRes.class)).thenReturn(token);
  }

  @Test void authenticateStoresReturnedToken() {
    response(new DiaryTokenRes("token", 1L, "User", List.of()));
    service.authenticate("http://diary", "login", "password");
    assertThat(holder.get()).isEqualTo("token");
  }

  @Test void authenticateRejectsMissingToken() {
    response(null);
    assertThatThrownBy(() -> service.authenticate("http://diary", "u", "p"))
        .isInstanceOf(ExternalResponseError.class);
  }

  @Test void loginValidatesSettings() {
    when(settings.getSingleton()).thenReturn(Optional.empty());
    assertThatThrownBy(service::login).isInstanceOf(ValidationException.class);
    AppSetting incomplete = new AppSetting();
    when(settings.getSingleton()).thenReturn(Optional.of(incomplete));
    assertThatThrownBy(service::login).isInstanceOf(ValidationException.class);
  }

  @Test void ensureTokenLogsInOnlyWhenMissingAndLogoutClears() {
    DiaryDebtAuthService spy = spy(service);
    doNothing().when(spy).login();
    spy.ensureToken(); verify(spy).login();
    holder.set("token"); spy.ensureToken(); verify(spy, times(1)).login();
    spy.logout(); assertThat(holder.isPresent()).isFalse();
  }
}
