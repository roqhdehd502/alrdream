package com.alrdream.spike;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
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
@RestController
public class PdfSmokeTestController {

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
