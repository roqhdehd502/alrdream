package com.alrdream.global.security;

import com.alrdream.global.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * [03] §4-5 — JWT 기반 stateless 인증. {@code /api/admin/**}는 {@code ROLE_ADMIN} 클레임을 가진 요청만 허용해
 * 별도 Admin 서버 없이 하나의 백엔드에서 권한을 분리한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	// Spring Boot 4.x가 자동 구성하는 ObjectMapper 빈은 Jackson 3.x(tools.jackson) 타입이라 여기서 필요한
	// Jackson 2.x(com.fasterxml.jackson) API와 맞지 않는다. ErrorResponse 직렬화 용도로만 쓰는 작은 범위라
	// 빈을 주입받는 대신 독립적으로 생성해 버전 결합을 피한다.
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/api/auth/signup",
								"/api/auth/login",
								"/api/auth/oauth/**",
								"/api/auth/refresh",
								"/actuator/health",
								"/swagger-ui/**",
								"/swagger-ui.html",
								"/v3/api-docs/**")
						.permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint((request, response, authException) ->
								writeError(response, 401, "UNAUTHORIZED", "인증이 필요합니다."))
						.accessDeniedHandler((request, response, accessDeniedException) ->
								writeError(response, 403, "FORBIDDEN", "접근 권한이 없습니다.")))
				.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	private void writeError(HttpServletResponse response, int status, String code, String message)
			throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(), new ErrorResponse(code, message));
	}
}
