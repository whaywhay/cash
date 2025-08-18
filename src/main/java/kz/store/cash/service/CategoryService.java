package kz.store.cash.service;

import java.util.List;
import java.util.Set;
import kz.store.cash.model.entity.Category;
import kz.store.cash.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

  private final CategoryRepository categoryRepository;

  public List<Category> findByParentCategoryId(String rootCode) {
    return categoryRepository.findByParentCategoryId(rootCode);
  }

  public List<Category> findRoots() {
    return categoryRepository.findRoots();
  }

  public List<Category> findAllByCategoryCodeIn(Set<String> categoryCodes) {
    return categoryRepository.findAllByCategoryCodeIn(categoryCodes);
  }

  public void saveAll(List<Category> newCategoryList) {
    categoryRepository.saveAll(newCategoryList);
  }
}
