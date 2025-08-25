package kz.store.cash.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import java.math.BigDecimal;
import kz.store.cash.model.entity.CashMovement;
import kz.store.cash.model.entity.CashShift;
import kz.store.cash.model.entity.User;
import kz.store.cash.model.enums.CashMovementType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CashMovementMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "created", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "lastUpdated", ignore = true)
  @Mapping(target = "lastUpdatedBy", ignore = true)
  @Mapping(target = "cashShift", source = "cashShift")
  @Mapping(target = "type", source = "type")
  @Mapping(target = "amount", source = "amount")
  @Mapping(target = "reason", source = "reason")
  @Mapping(target = "createdByUser", source = "user")
  CashMovement toCashMovement(CashShift cashShift, CashMovementType type,
      BigDecimal amount, String reason, User user);
}
