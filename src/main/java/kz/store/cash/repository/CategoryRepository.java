package kz.store.cash.repository;

import java.util.Optional;
import kz.store.cash.model.entity.Category;
import org.springframework.data.repository.CrudRepository;

public interface CategoryRepository extends CrudRepository<Category, Long> {

  Optional<Category> findByCategoryCode(String categoryCode);
}
