package com.alrdream.domain.survey.domain;

import com.alrdream.global.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * [03] §5 {@code survey_responses} 테이블. 제출 후 수정 불가(불변) — [02] §4/§7 "기획 수정"은 새 응답을 만드는
 * 것이지 기존 응답을 고치는 게 아니다. {@code submitted_at}만 있고(컬럼명이 {@link com.alrdream.global.jpa.BaseEntity}의
 * {@code created_at}과 달라 별도 선언), {@code updated_at}도 없다.
 */
@Getter
@Entity
@Table(name = "survey_responses")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyResponse {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@Column(name = "survey_definition_id", nullable = false)
	private UUID surveyDefinitionId;

	@Column(name = "workspace_id", nullable = false)
	private UUID workspaceId;

	// [03] §5 암호화 대상 — EncryptedStringConverter 참고. 암호문은 유효한 JSON이 아니라 TEXT 컬럼이다.
	@Convert(converter = EncryptedStringConverter.class)
	@Column(nullable = false)
	private String answers;

	@CreatedDate
	@Column(name = "submitted_at", nullable = false, updatable = false)
	private OffsetDateTime submittedAt;

	private SurveyResponse(UUID surveyDefinitionId, UUID workspaceId, String answers) {
		this.surveyDefinitionId = surveyDefinitionId;
		this.workspaceId = workspaceId;
		this.answers = answers;
	}

	public static SurveyResponse create(UUID surveyDefinitionId, UUID workspaceId, String answers) {
		return new SurveyResponse(surveyDefinitionId, workspaceId, answers);
	}
}
