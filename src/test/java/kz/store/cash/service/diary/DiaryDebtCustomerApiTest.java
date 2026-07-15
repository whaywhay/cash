package kz.store.cash.service.diary;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import java.util.Optional;
import java.util.concurrent.Callable;
import kz.store.cash.service.AppSettingService;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class DiaryDebtCustomerApiTest {
  @Test void getPageFailsClearlyWithoutBaseAddress() {
    DebtDiaryExecutor exec = mock(DebtDiaryExecutor.class);
    when(exec.call(any())).thenAnswer(i -> ((Callable<?>) i.getArgument(0)).call());
    AppSettingService settings = mock(AppSettingService.class);
    when(settings.getSingleton()).thenReturn(Optional.empty());
    DiaryDebtCustomerApi api = new DiaryDebtCustomerApi(mock(RestClient.class), exec,
        mock(DiaryDebtAuthService.class), settings);
    assertThatThrownBy(() -> api.getPage(1, null, null)).isInstanceOf(IllegalStateException.class);
  }
}
