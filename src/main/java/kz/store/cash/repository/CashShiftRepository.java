package kz.store.cash.repository;

import java.util.Optional;
import kz.store.cash.model.entity.CashShift;
import kz.store.cash.model.enums.CashShiftStatus;
import org.springframework.data.repository.CrudRepository;

public interface CashShiftRepository extends CrudRepository<CashShift, Long> {

  Optional<CashShift> findFirstByStatusOrderByShiftOpenedDateDesc(CashShiftStatus status);

  Optional<CashShift> findFirstByStatusOrderByShiftClosedDateDesc(CashShiftStatus status);
}
