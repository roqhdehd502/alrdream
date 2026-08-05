package com.alrdream.infrastructure.pdf;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import org.springframework.stereotype.Component;

/**
 * [03] §4-6 {@code HTML → PDF} 단계 (Phase 01 스파이크에서 검증된 OpenHTMLtoPDF 그대로).
 * PDFBox 기본 폰트는 한글 글리프가 없어, 렌더링마다 번들된 Noto Sans KR을 "Noto Sans KR" 패밀리로 등록한다
 * (전체 Noto Sans KR은 CJK 통합 한자를 모두 포함해 10MB를 넘어, 한글 음절/자모/라틴/구두점 범위만 서브셋한
 * 버전을 쓴다 — {@code fonts/README.md} 참고).
 */
@Component
public class HtmlToPdfConverter {

	private static final String FONT_FAMILY = "Noto Sans KR";

	public byte[] convert(String html) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			PdfRendererBuilder builder = new PdfRendererBuilder();
			builder.useFont(
					() -> getClass().getResourceAsStream("/fonts/NotoSansKR-Regular.ttf"),
					FONT_FAMILY, 400, FontStyle.NORMAL, true);
			builder.useFont(
					() -> getClass().getResourceAsStream("/fonts/NotoSansKR-Bold.ttf"),
					FONT_FAMILY, 700, FontStyle.NORMAL, true);
			builder.withHtmlContent(html, null);
			builder.toStream(out);
			builder.run();
			return out.toByteArray();
		} catch (Exception e) {
			throw new PdfRenderingException("PDF 렌더링에 실패했습니다.", e);
		}
	}
}
