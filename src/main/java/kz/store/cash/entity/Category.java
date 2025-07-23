package kz.store.cash.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "category", schema = "main")
public class Category extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "category_code", nullable = false)
  private String categoryCode;

  @Column(name = "category_name", nullable = false)
  private String categoryName;

  @Column(name = "full_path", nullable = false)
  private String fullPath;

  @Column(name = "parent_category_id")
  private String parentCategoryId;

}