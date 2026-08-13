package com.alrdream.global.security;

import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberRepository;
import com.alrdream.global.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code Authorization: Bearer <accessToken>} 헤더를 파싱해 SecurityContext에 인증 정보를 채운다.
 *
 * <p>Phase 19 — 제재(ban)된 사용자는 이미 발급받아 아직 만료되지 않은 액세스 토큰(최대 30분)이 있어도 즉시
 * 차단해야 의미가 있다("지금 서비스를 악용 중인 사용자를 막는다"는 제재 기능의 목적). 그래서 기존에는
 * 클레임만 신뢰하고 DB를 전혀 조회하지 않던 이 필터에 회원 조회 1건을 추가했다 — 이 앱 규모에서 PK 조회
 * 1건 추가는 무시할 수준이라고 판단했다(성능이 실제 문제가 되면 Redis 캐시로 옮길 수 있음, 지금은 과설계).
 *
 * <p>Phase 21 전수 점검 — 탈퇴({@code MemberService#withdraw})는 refresh token만 무효화할 뿐 이 필터에서
 * 검사되지 않아, 탈퇴 직전에 발급된 access token이 남은 유효기간(최대 30분) 동안 계속 정상 인증으로
 * 통과할 수 있었다. ban 검사와 같은 자리에서 {@code isWithdrawn()}도 함께 확인해 즉시 차단한다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String HEADER = "Authorization";
	private static final String PREFIX = "Bearer ";

	// SecurityConfig와 동일한 이유(Spring Boot 4.x 자동 구성 ObjectMapper는 Jackson 3.x라 여기 필요한
	// Jackson 2.x API와 안 맞음)로 독립적으로 생성한다.
	private final ObjectMapper objectMapper = new ObjectMapper();

	private final JwtTokenProvider jwtTokenProvider;
	private final MemberRepository memberRepository;

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, MemberRepository memberRepository) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.memberRepository = memberRepository;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String header = request.getHeader(HEADER);
		if (header != null && header.startsWith(PREFIX)) {
			String token = header.substring(PREFIX.length());
			try {
				Claims claims = jwtTokenProvider.parse(token);
				if (jwtTokenProvider.isAccessToken(claims)) {
					UUID memberId = jwtTokenProvider.getMemberId(claims);
					Member member = memberRepository.findById(memberId).orElse(null);
					if (member != null && member.isBanned()) {
						writeBannedError(response, member);
						return;
					}
					if (member != null && member.isWithdrawn()) {
						writeWithdrawnError(response);
						return;
					}
					MemberPrincipal principal = new MemberPrincipal(memberId, jwtTokenProvider.getRole(claims));
					var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
					var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
			} catch (JwtException | IllegalArgumentException ignored) {
				// 유효하지 않은/만료된 토큰 — 인증되지 않은 상태로 다음 필터로 진행, 이후 인가 단계에서 401 처리
			}
		}
		filterChain.doFilter(request, response);
	}

	private void writeBannedError(HttpServletResponse response, Member member) throws IOException {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(), new ErrorResponse("ACCOUNT_BANNED", member.banMessage()));
	}

	private void writeWithdrawnError(HttpServletResponse response) throws IOException {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(), new ErrorResponse("ACCOUNT_WITHDRAWN", "탈퇴한 계정입니다."));
	}
}
