package com.alrdream.global.error;

/** 권한은 있지만(로그인/소유권 확인됨) 플랜 등 조건 미충족으로 거부(403)를 의미하는 범용 예외. */
public class ForbiddenException extends RuntimeException {

	private final String code;

	public ForbiddenException(String message, String code) {
		super(message);
		this.code = code;
	}

	public String getCode() {
		return code;
	}
}
