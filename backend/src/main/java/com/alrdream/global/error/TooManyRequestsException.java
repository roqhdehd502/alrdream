package com.alrdream.global.error;

/** 요청 횟수 한도 초과(429)를 의미하는 범용 예외 — {@link IllegalArgumentException}이 400에 대응하는 것과 같은 역할. */
public class TooManyRequestsException extends RuntimeException {

	private final String code;

	public TooManyRequestsException(String message) {
		this(message, "QUOTA_EXCEEDED");
	}

	public TooManyRequestsException(String message, String code) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
