package com.alrdream.domain.analysis.domain;

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
 * [01] 7,8,9번, [03] §5 {@code analysis_versions}. 설문 없이 기획안 버전의 본문 자체를 입력으로 생성된다
 * ([03] §4-2) — 그래서 "생성"과 "수정"에 입력 차이가 없고, 같은 진입점을 다시 호출하면 새 버전이 만들어진다.
 */
@Getter
@Entity
@Table(name = "analysis_versions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisVersion extends SoftDeleteBaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@Column(name = "planning_version_id", nullable = false)
	private UUID planningVersionId;

	@Column(name = "version_no", nullable = false)
	private int versionNo;

	// [03] §5 암호화 대상. GENERATING 동안은 null.
	@Convert(converter = EncryptedStringConverter.class)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AnalysisVersionStatus status;

	private AnalysisVersion(UUID planningVersionId, int versionNo) {
		this.planningVersionId = planningVersionId;
		this.versionNo = versionNo;
		this.status = AnalysisVersionStatus.GENERATING;
	}

	public static AnalysisVersion create(UUID planningVersionId, int versionNo) {
		return new AnalysisVersion(planningVersionId, versionNo);
	}

	public void complete(String content) {
		this.content = content;
		this.status = AnalysisVersionStatus.COMPLETED;
	}

	public void fail() {
		this.status = AnalysisVersionStatus.FAILED;
	}
}
