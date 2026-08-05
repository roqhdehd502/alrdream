package com.alrdream.domain.survey.domain;

import com.alrdream.global.jpa.CreatedOnlyBaseEntity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/**
 * [03] §5 {@code survey_definitions} 테이블. Admin이 발행하며 발행 후 내용이 바뀌지 않는다(불변) —
 * 문항을 고치고 싶으면 새 버전을 발행한다({@code updated_at} 컬럼 자체가 없음, {@link CreatedOnlyBaseEntity} 참고).
 * {@code schema}는 원문 JSON 텍스트를 그대로 jsonb 컬럼에 저장한다({@link SurveySchema}를 이 프로젝트에 이미
 * 존재하는(Jackson 2.x/3.x 혼재) ObjectMapper 결합 문제를 피하기 위해 서비스 계층에서 직접 (역)직렬화한다).
 */
@Getter
@Entity
@Table(name = "survey_definitions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyDefinition extends CreatedOnlyBaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "survey_key", nullable = false)
	private SurveyKey surveyKey;

	@Column(nullable = false)
	private int version;

	@Column(nullable = false)
	private String title;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String schema;

	private SurveyDefinition(SurveyKey surveyKey, int version, String title, String schema) {
		this.surveyKey = surveyKey;
		this.version = version;
		this.title = title;
		this.schema = schema;
	}

	public static SurveyDefinition create(SurveyKey surveyKey, int version, String title, String schema) {
		return new SurveyDefinition(surveyKey, version, title, schema);
	}
}
