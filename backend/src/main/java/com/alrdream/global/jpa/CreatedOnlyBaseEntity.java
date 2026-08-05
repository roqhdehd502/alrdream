package com.alrdream.global.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * [03] §5 불변(immutable) 테이블(예: survey_definitions)이 상속한다 — {@code created_at}만 있고
 * {@code updated_at}은 없다. 컬럼명이 다른 {@code submitted_at} 같은 경우는 각 엔티티에서 직접 선언한다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class CreatedOnlyBaseEntity {

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;
}
