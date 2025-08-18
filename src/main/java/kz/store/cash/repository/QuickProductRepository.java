package kz.store.cash.repository;

import java.util.List;
import java.util.Optional;
import kz.store.cash.model.entity.QuickProduct;
import org.springframework.data.repository.CrudRepository;

public interface QuickProductRepository extends CrudRepository<QuickProduct, Long> {

  boolean existsByBarcode(String barcode);

  Optional<QuickProduct> findByBarcode(String barcode);

  List<QuickProduct> findAllByOrderBySortedDateDesc();

}
