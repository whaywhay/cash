package kz.store.cash.repository;

import java.util.List;
import java.util.Set;
import kz.store.cash.model.entity.Category;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface CategoryRepository extends CrudRepository<Category, Long> {

  List<Category> findAllByCategoryCodeIn(Set<String> categoryCodes);

  List<Category> findByParentCategoryId(String parentCategoryId);

  @Query("select c from Category c where c.parentCategoryId is null or c.parentCategoryId = ''")
  List<Category> findRoots();
}
