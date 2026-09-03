package com.alrdream.global.mail;

/**
 * Phase 23 — 인증 코드 메일 본문을 만드는 유틸리티. 이메일 클라이언트 호환성을 위해 외부 스타일시트/JS
 * 없이 인라인 스타일만 쓴다. 코드는 항상 서버가 생성한 6자리 숫자이고 제목/설명은 호출부의 고정 문자열이라
 * (수신자 입력값이 본문에 섞이지 않음) 별도 이스케이프는 필요 없다.
 */
public final class VerificationCodeEmailTemplate {

	private VerificationCodeEmailTemplate() {
	}

	public static String html(String title, String description, String code, int ttlMinutes) {
		return """
				<div style="max-width:480px;margin:0 auto;padding:36px 28px;
						font-family:'Apple SD Gothic Neo','Malgun Gothic',Helvetica,Arial,sans-serif;
						color:#1e293b;background:#ffffff;">
					<div style="text-align:center;margin-bottom:20px;">
						<span style="display:inline-block;width:40px;height:40px;border-radius:11px;
								background:#4f46e5;color:#ffffff;font-weight:700;font-size:16px;line-height:40px;">
							알
						</span>
					</div>
					<h1 style="font-size:19px;margin:0 0 8px;text-align:center;color:#0f172a;">%s</h1>
					<p style="font-size:14px;color:#64748b;text-align:center;margin:0 0 28px;line-height:1.5;">%s</p>
					<div style="background:#f1f5f9;border-radius:14px;padding:22px 16px;text-align:center;margin-bottom:16px;">
						<span style="font-size:34px;font-weight:700;letter-spacing:10px;color:#4f46e5;
								font-family:'SFMono-Regular',Consolas,Menlo,monospace;">%s</span>
					</div>
					<p style="font-size:13px;color:#94a3b8;text-align:center;margin:0;">%d분 후에 만료됩니다.</p>
					<hr style="border:none;border-top:1px solid #e2e8f0;margin:28px 0 16px;" />
					<p style="font-size:12px;color:#cbd5e1;text-align:center;margin:0;line-height:1.6;">
						본인이 요청하지 않았다면 이 메일을 무시해주세요.<br />알려드림
					</p>
				</div>
				"""
				.formatted(title, description, code, ttlMinutes);
	}

	public static String text(String description, String code, int ttlMinutes) {
		return description + "\n\n인증 코드: " + code + "\n\n" + ttlMinutes
				+ "분 후에 만료됩니다.\n본인이 요청하지 않았다면 이 메일을 무시해주세요.";
	}
}
