package com.alrdream.spike;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;

/**
 * [04_milestone.md] Phase 01 배포 스파이크 전용 엔드포인트.
 * Render 무료 인스턴스(0.1 vCPU/512MB)에서 OpenHTMLtoPDF 렌더링이 메모리 여유 안에서
 * 동작하는지 확인하기 위한 것으로, Phase 10에서 실제 PDF 파이프라인이 만들어지면 삭제한다.
 */
@Tag(name = "Spike", description = "배포 환경 검증용 임시 엔드포인트 — 해당 기능의 정식 Phase 구현 후 삭제 예정")
@RestController
public class PdfSmokeTestController {

	@Operation(
			summary = "PDF 렌더링 스모크 테스트",
			description = "OpenHTMLtoPDF로 더미 HTML을 PDF로 렌더링해 응답한다. Render 무료 인스턴스의 메모리 제약 안에서 렌더링이 정상 동작하는지 확인하는 용도.")
	@GetMapping("/spike/pdf-smoke-test")
	public ResponseEntity<byte[]> renderSmokeTestPdf() throws Exception {
		String html = """
				<html>
				  <body style="font-family: sans-serif;">
				    <h1>알려드림 PDF 스모크 테스트</h1>
				    <p>Render 인스턴스에서 OpenHTMLtoPDF 렌더링이 정상 동작하는지 확인하는 더미 문서입니다.</p>
				  </body>
				</html>
				""";

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PdfRendererBuilder builder = new PdfRendererBuilder();
		builder.withHtmlContent(html, null);
		builder.toStream(out);
		builder.run();

		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=smoke-test.pdf")
				.contentType(MediaType.APPLICATION_PDF)
				.body(out.toByteArray());
	}

}
