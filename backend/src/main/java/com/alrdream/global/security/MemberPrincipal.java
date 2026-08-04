package com.alrdream.global.security;

import com.alrdream.domain.member.domain.MemberRole;
import java.util.UUID;

/** JWT access token에서 추출한, 인증된 요청의 주체. */
public record MemberPrincipal(UUID memberId, MemberRole role) {
}
