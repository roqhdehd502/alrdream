package com.alrdream.domain.design.domain;

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

/**
 * [01] 10,11번, [03] §5 {@code design_versions}. 분석 버전 하나 + DESIGN 설문 응답 하나를 입력으로 생성된다.
 * "수정"은 [02] §7 원칙대로 새 DESIGN 설문 응답으로 새 버전을 만드는 것 — Planning과 동일한 패턴.
 */
@Getter
@Entity
@Table(name = "design_versions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DesignVersion extends SoftDeleteBaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@Column(name = "analysis_version_id", nullable = false)
	private UUID analysisVersionId;

	@Column(name = "survey_response_id", nullable = false)
	private UUID surveyResponseId;

	@Column(name = "version_no", nullable = false)
	private int versionNo;

	// [03] §5 암호화 대상. GENERATING 동안은 null.
	@Convert(converter = EncryptedStringConverter.class)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DesignVersionStatus status;

	private DesignVersion(UUID analysisVersionId, UUID surveyResponseId, int versionNo) {
		this.analysisVersionId = analysisVersionId;
		this.surveyResponseId = surveyResponseId;
		this.versionNo = versionNo;
		this.status = DesignVersionStatus.GENERATING;
	}

	public static DesignVersion create(UUID analysisVersionId, UUID surveyResponseId, int versionNo) {
		return new DesignVersion(analysisVersionId, surveyResponseId, versionNo);
	}

	public void complete(String content) {
		this.content = content;
		this.status = DesignVersionStatus.COMPLETED;
	}

	public void fail() {
		this.status = DesignVersionStatus.FAILED;
	}
}
