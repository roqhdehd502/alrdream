package com.alrdream.domain.member.api.dto;

import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberPlan;
import com.alrdream.domain.member.domain.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record MemberResponse(
		@Schema(description = "회원 ID") UUID id,
		@Schema(description = "이메일") String email,
		@Schema(description = "권한") MemberRole role,
		@Schema(description = "요금제") MemberPlan plan) {

	public static MemberResponse from(Member member) {
		return new MemberResponse(member.getId(), member.getEmail(), member.getRole(), member.getPlan());
	}
}
