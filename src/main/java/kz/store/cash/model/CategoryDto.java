package kz.store.cash.model;


import com.fasterxml.jackson.annotation.JsonProperty;

public record CategoryDto(
    @JsonProperty("category_id") String categoryId,
    @JsonProperty("category_name") String categoryName,
    @JsonProperty("parent_category_id") String parentCategoryId) {

}
