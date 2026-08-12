package com.alrdream.domain.member.api.dto;

import com.alrdream.domain.member.domain.AuthProvider;
import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberPlan;
import com.alrdream.domain.member.domain.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MemberResponse(
		@Schema(description = "회원 ID") UUID id,
		@Schema(description = "이메일") String email,
		@Schema(description = "표시 이름, 설정하지 않았으면 null") String name,
		@Schema(description = "가입 경로") AuthProvider provider,
		@Schema(description = "권한") MemberRole role,
		@Schema(description = "요금제") MemberPlan plan,
		@Schema(description = "구독과 무관하게 보장된 Pro 만료 시각(쿠폰/관리자 지급), 없으면 null") OffsetDateTime proExpiresAt) {

	public static MemberResponse from(Member member) {
		return new MemberResponse(
				member.getId(), member.getEmail(), member.getName(), member.getProvider(), member.getRole(),
				member.getPlan(), member.getProExpiresAt());
	}
}
