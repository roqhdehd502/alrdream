package com.alrdream.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** {@code Authorization: Bearer <accessToken>} 헤더를 파싱해 SecurityContext에 인증 정보를 채운다. */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String HEADER = "Authorization";
	private static final String PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
		this.jwtTokenProvider = jwtTokenProvider;
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
					MemberPrincipal principal = new MemberPrincipal(
							jwtTokenProvider.getMemberId(claims), jwtTokenProvider.getRole(claims));
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
}
