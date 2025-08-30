package kz.store.cash.service;

import java.util.Optional;
import kz.store.cash.mapper.AppSettingMapper;
import kz.store.cash.model.entity.AppSetting;
import kz.store.cash.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppSettingService {

  private final AppSettingRepository appSettingRepository;
  private final AppSettingMapper appSettingMapper;

  /**
   * Возвращает единственную запись, либо пусто.
   */
  public Optional<AppSetting> getSingleton() {
    return appSettingRepository.findTopByOrderByIdAsc();
  }

  /**
   * Загружает единственную запись или создаёт пустой объект (не сохранённый).
   */
  public AppSetting loadOrNew() {
    return getSingleton().orElseGet(AppSetting::new);
  }

  /**
   * Сохраняет/обновляет единственную запись. Если в БД нет записи — создаст новую. Если есть —
   * обновит поля существующей.
   */
  @Transactional
  public AppSetting saveSingleton(AppSetting appSetting) {
    AppSetting setting = appSettingRepository.findTopByOrderByIdAsc()
        .map(appSet -> {
          appSettingMapper.updateToAppSetting(appSet, appSetting);
          return appSet;
        })
        .orElseGet(() -> appSettingMapper.mapToAppSetting(appSetting));
    return appSettingRepository.save(setting);
  }
}