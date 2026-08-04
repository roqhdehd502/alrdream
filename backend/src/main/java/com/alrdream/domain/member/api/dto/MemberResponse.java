package com.alrdream.domain.member.api.dto;

import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberPlan;
import com.alrdream.domain.member.domain.MemberRole;
import java.util.UUID;

public record MemberResponse(UUID id, String email, MemberRole role, MemberPlan plan) {

	public static MemberResponse from(Member member) {
		return new MemberResponse(member.getId(), member.getEmail(), member.getRole(), member.getPlan());
	}
}
