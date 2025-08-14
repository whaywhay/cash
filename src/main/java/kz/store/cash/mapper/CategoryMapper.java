package kz.store.cash.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import kz.store.cash.model.CategoryDto;
import kz.store.cash.model.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {


  @Mapping(target = "categoryCode", source = "categoryId", qualifiedByName = "toTrim")
  @Mapping(target = "categoryName", source = "categoryName", qualifiedByName = "toTrim")
  @Mapping(target = "parentCategoryId", source = "parentCategoryId", qualifiedByName = "toTrim")
  void updateToCategory(@MappingTarget Category category, CategoryDto categoryDto);

  @Mapping(target = "createdBy", constant = "1C")
  @Mapping(target = "lastUpdatedBy", constant = "1C")
  @Mapping(target = "categoryCode", source = "categoryId", qualifiedByName = "toTrim")
  @Mapping(target = "categoryName", source = "categoryName", qualifiedByName = "toTrim")
  @Mapping(target = "parentCategoryId", source = "parentCategoryId", qualifiedByName = "toTrim")
  Category dtoToCategory(CategoryDto categoryDto);

  @Named("toTrim")
  static String toTrim(String value) {
    return value != null ? value.trim() : null;
  }
}
