package kz.store.cash.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Set;
import kz.store.cash.model.entity.Category;
import kz.store.cash.repository.CategoryRepository;
import org.junit.jupiter.api.Test;

class CategoryServiceTest {
  private final CategoryRepository repository = mock(CategoryRepository.class);
  private final CategoryService service = new CategoryService(repository);

  @Test void delegatesAllQueriesAndSave() {
    Category category = new Category();
    when(repository.findRoots()).thenReturn(List.of(category));
    when(repository.findByParentCategoryId("root")).thenReturn(List.of(category));
    when(repository.findAllByCategoryCodeIn(Set.of("A"))).thenReturn(List.of(category));
    assertThat(service.findRoots()).containsExactly(category);
    assertThat(service.findByParentCategoryId("root")).containsExactly(category);
    assertThat(service.findAllByCategoryCodeIn(Set.of("A"))).containsExactly(category);
    service.saveAll(List.of(category));
    verify(repository).saveAll(List.of(category));
  }
}
