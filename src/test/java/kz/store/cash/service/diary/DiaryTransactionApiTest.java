package kz.store.cash.service.diary;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import java.util.Optional;
import java.util.concurrent.Callable;
import kz.store.cash.model.diarydebt.DiaryTransaction;
import kz.store.cash.model.enums.DiaryOperationType;
import kz.store.cash.service.AppSettingService;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class DiaryTransactionApiTest {
  @Test void createSaleFailsWhenApplicationSettingsAreMissing() {
    DebtDiaryExecutor exec = mock(DebtDiaryExecutor.class);
    when(exec.call(any())).thenAnswer(i -> ((Callable<?>) i.getArgument(0)).call());
    AppSettingService settings = mock(AppSettingService.class);
    when(settings.getSingleton()).thenReturn(Optional.empty());
    DiaryTransactionApi api = new DiaryTransactionApi(mock(RestClient.class), exec,
        mock(DiaryDebtAuthService.class), settings);
    assertThatThrownBy(() -> api.createSale(mock(DiaryTransaction.class), DiaryOperationType.values()[0]))
        .isInstanceOf(Error.class);
  }
}
