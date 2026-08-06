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
		@Schema(description = "가입 시각") OffsetDateTime createdAt) {

	public static MemberAdminResponse from(Member member) {
		return new MemberAdminResponse(
				member.getId(), member.getEmail(), member.getProvider(), member.getRole(), member.getPlan(),
				member.getCreatedAt());
	}
}
