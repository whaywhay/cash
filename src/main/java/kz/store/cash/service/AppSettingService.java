package kz.store.cash.service;

import java.util.Optional;
import kz.store.cash.model.entity.AppSetting;
import kz.store.cash.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppSettingService {

  private final AppSettingRepository appSettingRepository;

  /** Возвращает единственную запись, либо пусто. */
  public Optional<AppSetting> getSingleton() {
    return appSettingRepository.findTopByOrderByIdAsc();
  }

  /** Загружает единственную запись или создаёт пустой объект (не сохранённый). */
  public AppSetting loadOrNew() {
    return getSingleton().orElseGet(AppSetting::new);
  }

  /**
   * Сохраняет/обновляет единственную запись.
   * Если в БД нет записи — создаст новую.
   * Если есть — обновит поля существующей.
   */
  @Transactional
  public AppSetting saveSingleton(AppSetting appSetting) {
    Optional<AppSetting> existingOpt = appSettingRepository.findTopByOrderByIdAsc();
    if (existingOpt.isPresent()) {
      AppSetting ex = existingOpt.get();
      ex.setOrgName(appSetting.getOrgName());
      ex.setBin(appSetting.getBin());
      ex.setAddress(appSetting.getAddress());
      ex.setSaleStore(appSetting.getSaleStore());
      return appSettingRepository.save(ex);
    } else {
      AppSetting created = new AppSetting();
      created.setOrgName(appSetting.getOrgName());
      created.setBin(appSetting.getBin());
      created.setAddress(appSetting.getAddress());
      created.setSaleStore(appSetting.getSaleStore());
      return appSettingRepository.save(created);
    }
  }
}