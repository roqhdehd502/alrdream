package com.alrdream.infrastructure.pdf;

import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * [03] §4-6 {@code content(JSON) → Thymeleaf 템플릿(HTML)} 단계. Spring Boot가 MVC 뷰 렌더링용으로 자동
 * 구성한 {@link TemplateEngine} 빈(`classpath:/templates/`, 접미사 `.html`)을 그대로 재사용해 문자열로 렌더링한다.
 */
@Component
public class PdfTemplateRenderer {

	private final TemplateEngine templateEngine;

	public PdfTemplateRenderer(TemplateEngine templateEngine) {
		this.templateEngine = templateEngine;
	}

	/** {@code templateName}은 {@code templates/} 기준 상대 경로(확장자 제외), 예: {@code "pdf/planning"}. */
	public String render(String templateName, Map<String, Object> model) {
		Context context = new Context();
		context.setVariables(model);
		return templateEngine.process(templateName, context);
	}
}
