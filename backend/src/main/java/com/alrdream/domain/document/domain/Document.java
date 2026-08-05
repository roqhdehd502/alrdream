package com.alrdream.domain.document.domain;

import com.alrdream.domain.ai.domain.AiTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/**
 * [03] §4-6, §5 {@code documents} — 기획/분석/설계 버전 하나당 생성되는 PDF 산출물. 콘텐츠가 완료되면 불변이므로
 * 버전당 최대 한 번만 렌더링/업로드하고, 이후에는 저장된 {@code storageKey}로 서명 URL만 다시 발급한다
 * (DocumentService). {@code generated_at}만 있고 {@code updated_at}이 없어 {@link com.alrdream.global.jpa.BaseEntity}는
 * 쓰지 않는다.
 */
@Getter
@Entity
@Table(name = "documents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document {

	@Id
	@UuidGenerator
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false)
	private AiTargetType sourceType;

	@Column(name = "source_id", nullable = false)
	private UUID sourceId;

	// 서명 URL은 만료되므로 영구 저장하지 않는다 — Supabase Storage 오브젝트 키만 저장하고, 조회 시마다 다시 서명한다.
	@Column(name = "file_url", nullable = false)
	private String storageKey;

	@Column(name = "generated_at", nullable = false)
	private OffsetDateTime generatedAt;

	private Document(AiTargetType sourceType, UUID sourceId, String storageKey) {
		this.sourceType = sourceType;
		this.sourceId = sourceId;
		this.storageKey = storageKey;
		this.generatedAt = OffsetDateTime.now();
	}

	public static Document create(AiTargetType sourceType, UUID sourceId, String storageKey) {
		return new Document(sourceType, sourceId, storageKey);
	}
}
