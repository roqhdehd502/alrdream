package com.alrdream.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI({@code /swagger-ui.html}) 상단 정보 및 JWT Bearer 인증 스킴 정의.
 * {@code /api/auth/me}, {@code /api/admin/**}처럼 인증이 필요한 API는 이 스킴으로 토큰을 넣어 테스트한다.
 */
@OpenAPIDefinition(info = @Info(
		title = "알려드림 API",
		description = "AI 기반 소상공인 사업 기획/분석/디자인 SaaS 백엔드 API",
		version = "v1"))
@SecurityScheme(
		name = "bearerAuth",
		type = SecuritySchemeType.HTTP,
		scheme = "bearer",
		bearerFormat = "JWT",
		in = SecuritySchemeIn.HEADER)
@Configuration
public class OpenApiConfig {
}
