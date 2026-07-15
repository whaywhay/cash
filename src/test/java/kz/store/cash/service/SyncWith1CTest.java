package kz.store.cash.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import java.util.Optional;
import kz.store.cash.fx.component.UiNotificationService;
import kz.store.cash.mapper.CategoryMapper;
import kz.store.cash.mapper.ProductMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class SyncWith1CTest {
  @Test void synchronizationRequiresConfiguredEndpoints() {
    AppSettingService settings = mock(AppSettingService.class);
    when(settings.getSingleton()).thenReturn(Optional.empty());
    SyncWith1C sync = new SyncWith1C(mock(RestClient.class), mock(CategoryService.class),
        mock(ProductService.class), mock(CategoryMapper.class), mock(ProductMapper.class),
        mock(UiNotificationService.class), settings);
    assertThatThrownBy(sync::syncCategory).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(sync::syncProduct).isInstanceOf(IllegalStateException.class);
  }
}
