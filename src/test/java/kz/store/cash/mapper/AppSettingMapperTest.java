package kz.store.cash.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import kz.store.cash.model.entity.AppSetting;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class AppSettingMapperTest {

  AppSettingMapper mapper = Mappers.getMapper(AppSettingMapper.class);

  @Test void mapToAppSettingCopiesEveryBusinessFieldIncludingDebtDiaryAccess() {
    AppSetting copy = mapper.mapToAppSetting(full());

    assertSameBusinessFields(copy, full());
  }

  @Test void mapToAppSettingProducesNewInstanceWithoutIdOrAuditData() {
    AppSetting source = full();
    source.setId(42L);
    source.setCreatedBy("someone");

    AppSetting copy = mapper.mapToAppSetting(source);

    assertThat(copy).isNotSameAs(source);
    assertThat(copy.getId()).isNull();
    assertThat(copy.getCreatedBy()).isEqualTo("system");
  }

  @Test void updateCopiesEveryBusinessFieldIncludingDebtDiaryAccess() {
    AppSetting target = new AppSetting();
    target.setId(7L);
    target.setOrgName("старое");
    target.setDebtDiaryLogin("old-login");

    mapper.updateToAppSetting(target, full());

    assertSameBusinessFields(target, full());
    assertThat(target.getId()).isEqualTo(7L);
  }

  @Test void updateKeepsIdAndAuditFieldsOfPersistedRecord() {
    AppSetting target = new AppSetting();
    target.setId(7L);
    target.setCreatedBy("admin");
    AppSetting source = full();
    source.setId(99L);
    source.setCreatedBy("hacker");

    mapper.updateToAppSetting(target, source);

    assertThat(target.getId()).isEqualTo(7L);
    assertThat(target.getCreatedBy()).isEqualTo("admin");
  }

  private static void assertSameBusinessFields(AppSetting actual, AppSetting expected) {
    assertThat(actual.getOrgName()).isEqualTo(expected.getOrgName());
    assertThat(actual.getBin()).isEqualTo(expected.getBin());
    assertThat(actual.getAddress()).isEqualTo(expected.getAddress());
    assertThat(actual.getSaleStore()).isEqualTo(expected.getSaleStore());
    assertThat(actual.getCategoryWebAddress()).isEqualTo(expected.getCategoryWebAddress());
    assertThat(actual.getProductWebAddress()).isEqualTo(expected.getProductWebAddress());
    assertThat(actual.getWebLogin()).isEqualTo(expected.getWebLogin());
    assertThat(actual.getWebPassword()).isEqualTo(expected.getWebPassword());
    assertThat(actual.getDebtDiaryBaseAddress()).isEqualTo(expected.getDebtDiaryBaseAddress());
    assertThat(actual.getDebtDiaryLogin()).isEqualTo(expected.getDebtDiaryLogin());
    assertThat(actual.getDebtDiaryPassword()).isEqualTo(expected.getDebtDiaryPassword());
  }

  private static AppSetting full() {
    AppSetting s = new AppSetting();
    s.setOrgName("ИП Иванов");
    s.setBin("123456789012");
    s.setAddress("Алматы");
    s.setSaleStore("Магазин 1");
    s.setCategoryWebAddress("http://1c/categories");
    s.setProductWebAddress("http://1c/products");
    s.setWebLogin("web");
    s.setWebPassword("web-pwd");
    s.setDebtDiaryBaseAddress("http://diary");
    s.setDebtDiaryLogin("cashier");
    s.setDebtDiaryPassword("diary-pwd");
    return s;
  }
}
