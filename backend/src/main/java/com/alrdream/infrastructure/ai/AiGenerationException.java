package com.alrdream.infrastructure.ai;

/** {@link AiClient} 호출/파싱 실패를 나타낸다. */
public class AiGenerationException extends RuntimeException {

	public AiGenerationException(String message) {
		super(message);
	}

	public AiGenerationException(String message, Throwable cause) {
		super(message, cause);
	}
}
