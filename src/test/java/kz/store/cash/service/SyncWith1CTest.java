package kz.store.cash.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import kz.store.cash.fx.component.UiNotificationService;
import kz.store.cash.handler.BusinessException;
import kz.store.cash.model.CategoryDto;
import kz.store.cash.model.ProductDto;
import kz.store.cash.model.entity.AppSetting;
import kz.store.cash.service.OneCSyncWriter.SyncCounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SyncWith1CTest {

  static final String CATEGORY_URL = "http://1c.local/categories";
  static final String PRODUCT_URL = "http://1c.local/products";

  AppSettingService settings = mock(AppSettingService.class);
  UiNotificationService ui = mock(UiNotificationService.class);
  OneCSyncWriter writer = mock(OneCSyncWriter.class);
  MockRestServiceServer server;
  SyncWith1C sync;
  AppSetting setting;

  @BeforeEach void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    sync = new SyncWith1C(builder.build(), ui, settings, writer);
    setting = new AppSetting();
    setting.setCategoryWebAddress(CATEGORY_URL);
    setting.setProductWebAddress(PRODUCT_URL);
    setting.setWebLogin("user");
    setting.setWebPassword("secret");
    when(settings.getSingleton()).thenReturn(Optional.of(setting));
  }

  @Test void synchronizationRequiresConfiguredEndpoints() {
    when(settings.getSingleton()).thenReturn(Optional.empty());
    assertThatThrownBy(sync::syncCategory).isInstanceOf(BusinessException.class);
    assertThatThrownBy(sync::syncProduct).isInstanceOf(BusinessException.class);
    verifyNoInteractions(writer);
  }

  @Test void synchronizationFailsWhenOnlyOneEndpointIsConfigured() {
    setting.setProductWebAddress("");
    assertThatThrownBy(sync::syncCategory).isInstanceOf(BusinessException.class);
    setting.setProductWebAddress(PRODUCT_URL);
    setting.setCategoryWebAddress(null);
    assertThatThrownBy(sync::syncProduct).isInstanceOf(BusinessException.class);
    verifyNoInteractions(writer);
  }

  @Test void categorySyncSendsBasicAuthParsesJsonAndPassesDtosToWriter() {
    String expectedAuth = "Basic " + Base64.getEncoder()
        .encodeToString("user:secret".getBytes(StandardCharsets.UTF_8));
    server.expect(requestTo(CATEGORY_URL))
        .andExpect(header("Authorization", expectedAuth))
        .andRespond(withSuccess("""
            [{"category_id":"A","category_name":"Milk","parent_category_id":"P"}]
            """, MediaType.APPLICATION_JSON));
    when(writer.applyCategorySync(any())).thenReturn(new SyncCounts(1, 0, 0));

    sync.syncCategory();

    server.verify();
    @SuppressWarnings("unchecked") ArgumentCaptor<List<CategoryDto>> captor = ArgumentCaptor.forClass(List.class);
    verify(writer).applyCategorySync(captor.capture());
    assertThat(captor.getValue()).containsExactly(new CategoryDto("A", "Milk", "P"));
    verify(ui).showInfo(any());
  }

  @Test void productSyncCallsProductEndpointAndParsesJson() {
    server.expect(requestTo(PRODUCT_URL))
        .andRespond(withSuccess("""
            [{"category_ref_id":"A","product_name":"Bread","barcode":"111","wholesale_price":120.50}]
            """, MediaType.APPLICATION_JSON));
    when(writer.applyProductSync(any())).thenReturn(new SyncCounts(1, 0, 0));

    sync.syncProduct();

    server.verify();
    @SuppressWarnings("unchecked") ArgumentCaptor<List<ProductDto>> captor = ArgumentCaptor.forClass(List.class);
    verify(writer).applyProductSync(captor.capture());
    assertThat(captor.getValue()).singleElement().satisfies(p -> {
      assertThat(p.barcode()).isEqualTo("111");
      assertThat(p.categoryRefId()).isEqualTo("A");
      assertThat(p.wholesalePrice()).isEqualByComparingTo("120.50");
    });
    verify(ui).showInfo(any());
  }

  @Test void missingCredentialsAreSentAsEmptyStrings() {
    setting.setWebLogin(null);
    setting.setWebPassword(null);
    String expectedAuth = "Basic " + Base64.getEncoder().encodeToString(":".getBytes(StandardCharsets.UTF_8));
    server.expect(requestTo(CATEGORY_URL)).andExpect(header("Authorization", expectedAuth))
        .andRespond(withSuccess("[{\"category_id\":\"A\"}]", MediaType.APPLICATION_JSON));
    when(writer.applyCategorySync(any())).thenReturn(new SyncCounts(0, 0, 0));

    sync.syncCategory();

    server.verify();
  }

  @Test void emptyResponseFromOneCIsReportedAndNothingIsWritten() {
    server.expect(requestTo(CATEGORY_URL)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    server.expect(requestTo(PRODUCT_URL)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    assertThatThrownBy(sync::syncCategory).isInstanceOf(BusinessException.class);
    assertThatThrownBy(sync::syncProduct).isInstanceOf(BusinessException.class);
    verifyNoInteractions(writer);
    verifyNoInteractions(ui);
  }

  @Test void emptyBodyFromOneCIsTreatedAsNoData() {
    server.expect(requestTo(CATEGORY_URL)).andRespond(withSuccess());

    assertThatThrownBy(sync::syncCategory).isInstanceOf(BusinessException.class);
    verifyNoInteractions(writer);
  }

  @Test void serverErrorFromOneCPropagatesAndNothingIsWritten() {
    server.expect(requestTo(CATEGORY_URL)).andRespond(withServerError());

    assertThatThrownBy(sync::syncCategory).isInstanceOf(RuntimeException.class);
    verifyNoInteractions(writer);
    verify(ui, never()).showInfo(any());
  }

  @Test void syncAllRunsCategoriesBeforeProducts() {
    server.expect(requestTo(CATEGORY_URL)).andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(withSuccess("[{\"category_id\":\"A\"}]", MediaType.APPLICATION_JSON));
    server.expect(requestTo(PRODUCT_URL))
        .andRespond(withSuccess("[{\"barcode\":\"111\"}]", MediaType.APPLICATION_JSON));
    when(writer.applyCategorySync(any())).thenReturn(new SyncCounts(1, 0, 0));
    when(writer.applyProductSync(any())).thenReturn(new SyncCounts(1, 0, 0));

    sync.syncAll1C();

    server.verify();
    var order = inOrder(writer);
    order.verify(writer).applyCategorySync(any());
    order.verify(writer).applyProductSync(any());
  }

  @Test void syncAllStopsBeforeProductsWhenCategoriesFail() {
    server.expect(requestTo(CATEGORY_URL)).andRespond(withServerError());

    assertThatThrownBy(sync::syncAll1C).isInstanceOf(RuntimeException.class);
    server.verify();
    verify(writer, never()).applyProductSync(any());
  }
}
