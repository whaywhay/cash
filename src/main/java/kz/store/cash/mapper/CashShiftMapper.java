package kz.store.cash.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import kz.store.cash.model.entity.CashShift;
import kz.store.cash.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE, imports = {
    LocalDateTime.class})
public interface CashShiftMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "created", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "lastUpdated", ignore = true)
  @Mapping(target = "lastUpdatedBy", ignore = true)
  @Mapping(target = "openedUser", source = "user")
  @Mapping(target = "cashDuringOpening", source = "cashDuringOpening")
  @Mapping(target = "status", constant = "OPENED")
  @Mapping(target = "shiftOpenedDate", expression = "java(LocalDateTime.now())")
  @Mapping(target = "sumCash", expression = "java(BigDecimal.ZERO)")
  @Mapping(target = "sumCard", expression = "java(BigDecimal.ZERO)")
  @Mapping(target = "leftInDrawer", expression = "java(BigDecimal.ZERO)")
  CashShift toOpenCashShift(User user, BigDecimal cashDuringOpening);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "created", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "lastUpdated", ignore = true)
  @Mapping(target = "lastUpdatedBy", ignore = true)
  @Mapping(target = "closedBy", source = "user")
  @Mapping(target = "status", constant = "CLOSED")
  @Mapping(target = "shiftClosedDate", expression = "java(LocalDateTime.now())")
  @Mapping(target = "sumCash", source = "sumCash")
  @Mapping(target = "sumCard", source = "sumCard")
  @Mapping(target = "leftInDrawer", source = "leftInDrawer")
  @Mapping(target = "note", source = "note")
  @Mapping(target = "sumReturnCash", source = "sumReturnCash")
  @Mapping(target = "sumReturnCard", source = "sumReturnCard")
  void toCloseCashShift(@MappingTarget CashShift cashShift, User user, BigDecimal leftInDrawer,
      BigDecimal sumCash, BigDecimal sumCard, BigDecimal sumReturnCash, BigDecimal sumReturnCard,
      String note);
}
