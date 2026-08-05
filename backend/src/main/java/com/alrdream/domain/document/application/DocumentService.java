package com.alrdream.domain.document.application;

import com.alrdream.domain.ai.domain.AiTargetType;
import com.alrdream.domain.document.api.dto.DocumentResponse;
import com.alrdream.domain.document.domain.Document;
import com.alrdream.domain.document.domain.DocumentRepository;
import com.alrdream.infrastructure.pdf.HtmlToPdfConverter;
import com.alrdream.infrastructure.pdf.PdfTemplateRenderer;
import com.alrdream.infrastructure.storage.StorageClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [03] §4-6 — 기획/분석/설계 세 도메인이 공유하는 단일 PDF 생성 진입점. {@code content(JSON) → Thymeleaf →
 * OpenHTMLtoPDF → Supabase Storage 업로드} 파이프라인을 수행하고 {@code documents}에 기록한다. 완료된 버전의
 * content는 불변이므로 소스당 최초 한 번만 렌더링/업로드하고, 이미 있으면 저장된 오브젝트 키로 서명 URL만 다시
 * 발급한다(AiGenerationJobService/UsageQuotaService와 같은 "공용 진입점 하나" 패턴).
 */
@Service
@Transactional(readOnly = true)
public class DocumentService {

	private static final String CONTENT_TYPE_PDF = "application/pdf";

	private final DocumentRepository documentRepository;
	private final PdfTemplateRenderer pdfTemplateRenderer;
	private final HtmlToPdfConverter htmlToPdfConverter;
	private final StorageClient storageClient;
	private final Duration signedUrlTtl;

	// SurveyDefinitionService 등과 동일한 이유(Jackson 2.x/3.x 혼재)로 독립 생성.
	private final ObjectMapper objectMapper = new ObjectMapper();

	public DocumentService(
			DocumentRepository documentRepository,
			PdfTemplateRenderer pdfTemplateRenderer,
			HtmlToPdfConverter htmlToPdfConverter,
			StorageClient storageClient,
			@Value("${app.storage.supabase.signed-url-ttl-minutes}") long signedUrlTtlMinutes) {
		this.documentRepository = documentRepository;
		this.pdfTemplateRenderer = pdfTemplateRenderer;
		this.htmlToPdfConverter = htmlToPdfConverter;
		this.storageClient = storageClient;
		this.signedUrlTtl = Duration.ofMinutes(signedUrlTtlMinutes);
	}

	/**
	 * {@code sourceType}+{@code sourceId}의 PDF를 조회하거나(이미 생성됨) 새로 생성한다.
	 *
	 * @param templateName {@code templates/} 기준 상대 경로(확장자 제외), 예: {@code "pdf/planning"}
	 * @param contentJson  완료된 버전의 content(JSON 문자열) — 템플릿 모델의 {@code content} 변수로 들어간다
	 * @param extraModel   {@code content} 외에 템플릿에서 쓸 추가 변수 (예: versionNo)
	 */
	@Transactional
	public DocumentResponse getOrGenerate(
			AiTargetType sourceType, UUID sourceId, String templateName, String contentJson, Map<String, Object> extraModel) {
		return documentRepository.findFirstBySourceTypeAndSourceIdOrderByGeneratedAtDesc(sourceType, sourceId)
				.map(this::toResponse)
				.orElseGet(() -> generate(sourceType, sourceId, templateName, contentJson, extraModel));
	}

	private DocumentResponse generate(
			AiTargetType sourceType, UUID sourceId, String templateName, String contentJson, Map<String, Object> extraModel) {
		Map<String, Object> model = new HashMap<>(extraModel);
		model.put("content", parseContent(contentJson));

		String html = pdfTemplateRenderer.render(templateName, model);
		byte[] pdfBytes = htmlToPdfConverter.convert(html);

		String storageKey = "documents/%s/%s.pdf".formatted(sourceType.name().toLowerCase(), sourceId);
		storageClient.upload(storageKey, pdfBytes, CONTENT_TYPE_PDF);

		Document document = documentRepository.save(Document.create(sourceType, sourceId, storageKey));
		return toResponse(document);
	}

	private DocumentResponse toResponse(Document document) {
		String downloadUrl = storageClient.generateDownloadUrl(document.getStorageKey(), signedUrlTtl);
		return new DocumentResponse(downloadUrl, document.getGeneratedAt());
	}

	private Map<String, Object> parseContent(String contentJson) {
		try {
			return objectMapper.readValue(contentJson, new TypeReference<Map<String, Object>>() {
			});
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("완료된 콘텐츠를 파싱할 수 없습니다.", e);
		}
	}
}
