package kz.store.cash.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import java.util.Optional;
import kz.store.cash.fx.component.UiNotificationService;
import kz.store.cash.handler.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class SyncWith1CTest {
  @Test void synchronizationRequiresConfiguredEndpoints() {
    AppSettingService settings = mock(AppSettingService.class);
    when(settings.getSingleton()).thenReturn(Optional.empty());
    SyncWith1C sync = new SyncWith1C(mock(RestClient.class), mock(UiNotificationService.class),
        settings, mock(OneCSyncWriter.class));
    assertThatThrownBy(sync::syncCategory).isInstanceOf(BusinessException.class);
    assertThatThrownBy(sync::syncProduct).isInstanceOf(BusinessException.class);
  }
}
