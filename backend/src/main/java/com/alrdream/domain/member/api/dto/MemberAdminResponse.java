package com.alrdream.domain.member.api.dto;

import com.alrdream.domain.member.domain.AuthProvider;
import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberPlan;
import com.alrdream.domain.member.domain.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

/** [03] §2-1 Admin의 CS 대응용 사용자 조회 응답 — 자체 {@code /api/auth/me}용 {@link MemberResponse}와 달리 가입 경로/가입일도 포함한다. */
public record MemberAdminResponse(
		@Schema(description = "회원 ID") UUID id,
		@Schema(description = "이메일") String email,
		@Schema(description = "가입 경로") AuthProvider provider,
		@Schema(description = "권한") MemberRole role,
		@Schema(description = "요금제") MemberPlan plan,
		@Schema(description = "구독과 무관하게 보장된 Pro 만료 시각(쿠폰/관리자 지급), 없으면 null") OffsetDateTime proExpiresAt,
		@Schema(description = "현재 제재 여부(일시/영구 포함)") boolean banned,
		@Schema(description = "영구 제재 여부") boolean permanentBan,
		@Schema(description = "일시 제재 해제 시각, 없으면 null") OffsetDateTime tempBanUntil,
		@Schema(description = "가입 시각") OffsetDateTime createdAt) {

	public static MemberAdminResponse from(Member member) {
		return new MemberAdminResponse(
				member.getId(), member.getEmail(), member.getProvider(), member.getRole(), member.getPlan(),
				member.getProExpiresAt(), member.isBanned(), member.isPermanentBan(), member.getTempBanUntil(),
				member.getCreatedAt());
	}
}
