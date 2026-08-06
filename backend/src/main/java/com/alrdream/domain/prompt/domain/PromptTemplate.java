package com.alrdream.domain.prompt.domain;

import com.alrdream.domain.ai.domain.AiTargetType;
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
import org.hibernate.annotations.UuidGenerator;

/**
 * [03] §2-1 {@code prompt_templates} 테이블 — 기획/분석/설계 생성에 쓰이는 시스템 프롬프트/Tool
 * 스키마를 Admin이 편집할 수 있도록 DB로 옮긴 것(이전에는 {@code PlanningGenerationSpec} 등 Java 코드에
 * 하드코딩돼 있었다). {@code survey_definitions}와 동일하게 발행 후 내용이 바뀌지 않는 불변 버전 관리 방식 —
 * 문항을 고치고 싶으면 새 버전을 발행한다({@link CreatedOnlyBaseEntity} 참고).
 */
@Getter
@Entity
@Table(name = "prompt_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptTemplate extends CreatedOnlyBaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "prompt_type", nullable = false)
	private AiTargetType promptType;

	@Column(nullable = false)
	private int version;

	@Column(name = "tool_name", nullable = false)
	private String toolName;

	@Column(name = "tool_description", nullable = false)
	private String toolDescription;

	@Column(name = "system_prompt", nullable = false)
	private String systemPrompt;

	@Column(name = "schema_json", nullable = false)
	private String schemaJson;

	private PromptTemplate(
			AiTargetType promptType, int version, String toolName, String toolDescription,
			String systemPrompt, String schemaJson) {
		this.promptType = promptType;
		this.version = version;
		this.toolName = toolName;
		this.toolDescription = toolDescription;
		this.systemPrompt = systemPrompt;
		this.schemaJson = schemaJson;
	}

	public static PromptTemplate create(
			AiTargetType promptType, int version, String toolName, String toolDescription,
			String systemPrompt, String schemaJson) {
		return new PromptTemplate(promptType, version, toolName, toolDescription, systemPrompt, schemaJson);
	}
}
