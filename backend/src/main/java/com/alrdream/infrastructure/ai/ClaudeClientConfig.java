package com.alrdream.infrastructure.ai;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class ClaudeClientConfig {

	@Bean
	ClaudeApi claudeApi(
			@Value("${app.ai.claude.base-url}") String baseUrl,
			@Value("${app.ai.claude.api-key}") String apiKey,
			@Value("${app.ai.claude.api-version}") String apiVersion,
			@Value("${app.ai.claude.connect-timeout-seconds}") long connectTimeoutSeconds,
			@Value("${app.ai.claude.read-timeout-seconds}") long readTimeoutSeconds) {
		// Spring Boot 4.x가 자동 구성하는 공용 ObjectMapper 빈과 얽히지 않도록, Claude 전용 RestClient에는
		// 독립적인 JsonMapper를 명시적으로 지정한다. jjwt-jackson 등 레거시 제약이 없는 새 컴포넌트라
		// Jackson 2.x가 아니라 Spring 7의 현재 권장 스택인 Jackson 3(tools.jackson)을 그대로 쓴다.
		JsonMapper jsonMapper = JsonMapper.builder()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.build();

		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
		// LLM 생성은 수십 초가 걸릴 수 있어 넉넉히 잡는다 — 이 호출 자체가 이미 Virtual Thread 비동기 Job
		// 안에서 실행되므로(AiGenerationJobService) 요청 스레드를 오래 붙잡지 않는다.
		requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));

		RestClient restClient = RestClient.builder()
				.baseUrl(baseUrl)
				.defaultHeader("x-api-key", apiKey)
				.defaultHeader("anthropic-version", apiVersion)
				.requestFactory(requestFactory)
				.configureMessageConverters(converters -> converters.withJsonConverter(new JacksonJsonHttpMessageConverter(jsonMapper)))
				.build();

		return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
				.build()
				.createClient(ClaudeApi.class);
	}
}
