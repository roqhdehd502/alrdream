package com.alrdream.infrastructure.ai;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

/** [03] §4-3 — Spring 6 {@code @HttpExchange} 선언형 클라이언트. 인증 헤더 등은 {@link ClaudeClientConfig}의 RestClient가 담당한다. */
interface ClaudeApi {

	@PostExchange("/v1/messages")
	ClaudeMessagesResponse createMessage(@RequestBody ClaudeMessagesRequest request);
}
