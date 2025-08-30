package kz.store.cash.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import kz.store.cash.model.entity.AppSetting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppSettingMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "created", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "lastUpdated", ignore = true)
  @Mapping(target = "lastUpdatedBy", ignore = true)
  @Mapping(target = "orgName", source = "orgName")
  @Mapping(target = "bin", source = "bin")
  @Mapping(target = "address", source = "address")
  @Mapping(target = "saleStore", source = "saleStore")
  @Mapping(target = "categoryWebAddress", source = "categoryWebAddress")
  @Mapping(target = "productWebAddress", source = "productWebAddress")
  @Mapping(target = "webLogin", source = "webLogin")
  @Mapping(target = "webPassword", source = "webPassword")
  void updateToAppSetting(@MappingTarget AppSetting setting, AppSetting appSetting);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "created", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "lastUpdated", ignore = true)
  @Mapping(target = "lastUpdatedBy", ignore = true)
  @Mapping(target = "orgName", source = "orgName")
  @Mapping(target = "bin", source = "bin")
  @Mapping(target = "address", source = "address")
  @Mapping(target = "saleStore", source = "saleStore")
  @Mapping(target = "categoryWebAddress", source = "categoryWebAddress")
  @Mapping(target = "productWebAddress", source = "productWebAddress")
  @Mapping(target = "webLogin", source = "webLogin")
  @Mapping(target = "webPassword", source = "webPassword")
  AppSetting mapToAppSetting(AppSetting appSetting);
}
