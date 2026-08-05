package com.alrdream.domain.ai.domain;

import com.alrdream.global.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/** [03] §4-4, §5 {@code ai_generation_jobs} 테이블. target_id는 target_type에 따라 planning/analysis/design_versions 중 하나를 가리키는 다형 참조라 DB FK를 걸지 않는다. */
@Getter
@Entity
@Table(name = "ai_generation_jobs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiGenerationJob extends BaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_type", nullable = false)
	private AiTargetType targetType;

	@Column(name = "target_id", nullable = false)
	private UUID targetId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private JobStatus status;

	@Column(name = "error_message")
	private String errorMessage;

	private AiGenerationJob(UUID userId, AiTargetType targetType, UUID targetId) {
		this.userId = userId;
		this.targetType = targetType;
		this.targetId = targetId;
		this.status = JobStatus.PENDING;
	}

	public static AiGenerationJob create(UUID userId, AiTargetType targetType, UUID targetId) {
		return new AiGenerationJob(userId, targetType, targetId);
	}

	public void markProcessing() {
		this.status = JobStatus.PROCESSING;
	}

	public void markCompleted() {
		this.status = JobStatus.COMPLETED;
	}

	public void markFailed(String errorMessage) {
		this.status = JobStatus.FAILED;
		this.errorMessage = errorMessage;
	}
}
