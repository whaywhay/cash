package kz.store.cash.repository;

import java.util.List;
import kz.store.cash.model.entity.CashMovement;
import org.springframework.data.repository.CrudRepository;

public interface CashMovementRepository extends CrudRepository<CashMovement, Long> {

  List<CashMovement> findAllByCashShiftIdOrderByCreatedDesc(Long shiftId);
}
