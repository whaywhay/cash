package kz.store.cash.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class BaseEntity {

  @CreatedDate
  @Column(name = "created", updatable = false, nullable = false)
  private LocalDateTime created;

  @CreatedBy
  @Column(name = "created_by", updatable = false, nullable = false)
  protected String createdBy = "system";

  @LastModifiedDate
  @Column(name = "last_upd", nullable = false)
  private LocalDateTime lastUpdated;

  @LastModifiedBy
  @Column(name = "last_upd_by", nullable = false)
  protected String lastUpdatedBy = "system";
}
