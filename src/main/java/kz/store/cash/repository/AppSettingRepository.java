package kz.store.cash.repository;

import java.util.Optional;
import kz.store.cash.model.entity.AppSetting;
import org.springframework.data.repository.CrudRepository;

public interface AppSettingRepository extends CrudRepository<AppSetting, Long> {

  Optional<AppSetting> findTopByOrderByIdAsc();

}
