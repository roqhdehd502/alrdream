package com.alrdream.domain.planning.domain;

import com.alrdream.global.jpa.SoftDeleteBaseEntity;
import com.alrdream.global.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

/** [01] 2,5,6번, [03] §5 {@code planning_versions}. 설문 응답 1건에서 생성되는 기획안의 각 버전. */
@Getter
@Entity
@Table(name = "planning_versions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanningVersion extends SoftDeleteBaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@Column(name = "workspace_id", nullable = false)
	private UUID workspaceId;

	@Column(name = "survey_response_id", nullable = false)
	private UUID surveyResponseId;

	@Column(name = "version_no", nullable = false)
	private int versionNo;

	// [03] §5 암호화 대상 — 사업 아이디어가 담긴 AI 생성 결과. GENERATING 동안은 null.
	@Convert(converter = EncryptedStringConverter.class)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PlanningVersionStatus status;

	private PlanningVersion(UUID workspaceId, UUID surveyResponseId, int versionNo) {
		this.workspaceId = workspaceId;
		this.surveyResponseId = surveyResponseId;
		this.versionNo = versionNo;
		this.status = PlanningVersionStatus.GENERATING;
	}

	public static PlanningVersion create(UUID workspaceId, UUID surveyResponseId, int versionNo) {
		return new PlanningVersion(workspaceId, surveyResponseId, versionNo);
	}

	public void complete(String content) {
		this.content = content;
		this.status = PlanningVersionStatus.COMPLETED;
	}

	public void fail() {
		this.status = PlanningVersionStatus.FAILED;
	}
}
