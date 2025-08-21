package kz.store.cash.repository;

import kz.store.cash.model.entity.CashMovement;
import org.springframework.data.repository.CrudRepository;

public interface CashMovementRepository extends CrudRepository<CashMovement, Long> {

}
