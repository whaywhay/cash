package kz.store.cash.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import kz.store.cash.config.ApiUrlProperties;
import kz.store.cash.mapper.CategoryMapper;
import kz.store.cash.model.CategoryDto;
import kz.store.cash.model.entity.Category;
import kz.store.cash.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryAndProduct1C {

  private final RestClient restClient;
  private final ApiUrlProperties apiUrlProperties;
  private final CategoryRepository categoryRepository;
  private final CategoryMapper categoryMapper;


  public List<CategoryDto> getCategoryDto() {
    var properties = apiUrlProperties.urlBasic().get("category1c");
    URI uri = properties.baseUrl();
    String basicAuth = "Basic " + Base64.getEncoder()
        .encodeToString((properties.username() + ":" + properties.password()).getBytes(
            StandardCharsets.UTF_8));
    return restClient.get().uri(uri)
        .header("Authorization", basicAuth)
        .retrieve()
        .body(new ParameterizedTypeReference<>() {
        });

  }

  public void syncCategory() {
    var dtos = getCategoryDto();
    if (dtos == null || dtos.isEmpty()) {
      log.warn("Категории из 1С не получены или пусты");
      return;
    }

    for (CategoryDto dto : dtos) {
      var code = trim(dto.categoryId());
      if (code == null) {
        continue;
      }
      var optCategory = categoryRepository.findByCategoryCode(code);
      optCategory.ifPresentOrElse(category -> {
            categoryMapper.updateToCategory(category, dto);
            categoryRepository.save(category);
          },
          () -> {
            Category category = categoryMapper.dtoToCategory(dto);
            categoryRepository.save(category);
          });
    }
  }

  private static String trim(String s) {
    return s == null ? null : s.trim();
  }

}
