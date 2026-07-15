package kz.store.cash.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Optional;
import kz.store.cash.mapper.AppSettingMapper;
import kz.store.cash.model.entity.AppSetting;
import kz.store.cash.repository.AppSettingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppSettingServiceTest {
  @Mock AppSettingRepository repository;
  @Mock AppSettingMapper mapper;
  @InjectMocks AppSettingService service;

  @Test void loadOrNewReturnsPersistedSingleton() {
    AppSetting existing = new AppSetting();
    when(repository.findTopByOrderByIdAsc()).thenReturn(Optional.of(existing));
    assertThat(service.loadOrNew()).isSameAs(existing);
  }

  @Test void loadOrNewCreatesUnsavedSettingsWhenMissing() {
    when(repository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());
    assertThat(service.loadOrNew()).isNotNull();
    verify(repository, never()).save(any());
  }

  @Test void saveSingletonUpdatesExistingEntity() {
    AppSetting input = new AppSetting();
    AppSetting existing = new AppSetting();
    when(repository.findTopByOrderByIdAsc()).thenReturn(Optional.of(existing));
    when(repository.save(existing)).thenReturn(existing);

    assertThat(service.saveSingleton(input)).isSameAs(existing);
    verify(mapper).updateToAppSetting(existing, input);
    verify(mapper, never()).mapToAppSetting(any());
  }

  @Test void saveSingletonMapsNewEntityWhenMissing() {
    AppSetting input = new AppSetting();
    AppSetting mapped = new AppSetting();
    when(repository.findTopByOrderByIdAsc()).thenReturn(Optional.empty());
    when(mapper.mapToAppSetting(input)).thenReturn(mapped);
    when(repository.save(mapped)).thenReturn(mapped);

    assertThat(service.saveSingleton(input)).isSameAs(mapped);
    verify(mapper, never()).updateToAppSetting(any(), any());
  }
}
