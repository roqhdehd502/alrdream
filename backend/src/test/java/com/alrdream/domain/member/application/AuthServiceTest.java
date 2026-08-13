package com.alrdream.domain.member.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alrdream.domain.member.domain.AuthProvider;
import com.alrdream.domain.member.domain.Member;
import com.alrdream.domain.member.domain.MemberRepository;
import com.alrdream.domain.member.infrastructure.AppleIdTokenVerifierAdapter;
import com.alrdream.domain.member.infrastructure.GoogleIdTokenVerifierAdapter;
import com.alrdream.global.error.ForbiddenException;
import com.alrdream.global.security.JwtTokenProvider;
import com.alrdream.global.security.RefreshTokenStore;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * {@link AuthService} 단위 테스트 — 리포지토리/저장소는 Mockito로 대체하고, 실제 JWT 서명/파싱 로직은
 * {@link JwtTokenProvider} 실 객체를 써서 refresh 흐름까지 end-to-end로 검증한다(Docker/DB 불필요).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final String RAW_PASSWORD = "correct-password";

	@Mock
	private MemberRepository memberRepository;
	@Mock
	private RefreshTokenStore refreshTokenStore;
	@Mock
	private GoogleIdTokenVerifierAdapter googleIdTokenVerifier;
	@Mock
	private AppleIdTokenVerifierAdapter appleIdTokenVerifier;

	// 프로덕션(SecurityConfig)과 동일한 BCryptPasswordEncoder를 실 객체로 써서 matches()까지 실제로 검증한다.
	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private final JwtTokenProvider jwtTokenProvider =
			new JwtTokenProvider("test-secret-key-for-unit-tests-needs-32-bytes-min", 1800, 604800);

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(
				memberRepository, passwordEncoder, jwtTokenProvider, refreshTokenStore,
				googleIdTokenVerifier, appleIdTokenVerifier);
	}

	private Member localMemberWithPassword() {
		// 실제로는 JPA가 저장 시점에 id를 채워주지만(@GeneratedValue), 순수 단위 테스트는 영속화 없이 엔티티를
		// 만들기 때문에 JWT subject 등 id가 필요한 로직을 검증하려면 리플렉션으로 직접 채워야 한다.
		Member member = Member.createLocal("user@example.com", passwordEncoder.encode(RAW_PASSWORD));
		ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
		return member;
	}

	@Test
	void login_존재하지_않는_이메일이면_예외() {
		when(memberRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login("nobody@example.com", "pw"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("올바르지 않습니다");
	}

	@Test
	void login_비밀번호가_틀리면_예외() {
		Member member = localMemberWithPassword();
		when(memberRepository.findByEmail("user@example.com")).thenReturn(Optional.of(member));

		assertThatThrownBy(() -> authService.login("user@example.com", "wrong-password"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void login_OAuth로_가입한_계정은_비밀번호_로그인_거부() {
		Member member = Member.createOAuth("user@example.com", AuthProvider.GOOGLE, "sub-1");
		when(memberRepository.findByEmail("user@example.com")).thenReturn(Optional.of(member));

		assertThatThrownBy(() -> authService.login("user@example.com", "아무값"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void login_제재된_계정은_비밀번호가_맞아도_거부() {
		Member member = localMemberWithPassword();
		member.banPermanently();
		when(memberRepository.findByEmail("user@example.com")).thenReturn(Optional.of(member));

		assertThatThrownBy(() -> authService.login("user@example.com", RAW_PASSWORD))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void login_성공하면_토큰을_발급하고_refreshTokenStore에_저장() {
		Member member = localMemberWithPassword();
		when(memberRepository.findByEmail("user@example.com")).thenReturn(Optional.of(member));

		AuthService.TokenIssueResult result = authService.login("user@example.com", RAW_PASSWORD);

		assertThat(result.accessToken()).isNotBlank();
		assertThat(result.refreshToken()).isNotBlank();
		verify(refreshTokenStore).save(any(), any(), anyLong());
	}

	@Test
	void verifyPassword_LOCAL_계정이고_비밀번호가_맞으면_통과() {
		Member member = localMemberWithPassword();
		when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

		authService.verifyPassword(member.getId(), RAW_PASSWORD); // 예외 없이 통과하면 성공
	}

	@Test
	void verifyPassword_비밀번호가_틀리면_예외() {
		Member member = localMemberWithPassword();
		when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

		assertThatThrownBy(() -> authService.verifyPassword(member.getId(), "wrong"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void verifyPassword_OAuth_계정은_거부() {
		Member member = Member.createOAuth("user@example.com", AuthProvider.GOOGLE, "sub-1");
		when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

		assertThatThrownBy(() -> authService.verifyPassword(member.getId(), "아무값"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void verifyPassword_새_토큰을_발급하지_않아_refreshTokenStore를_건드리지_않음() {
		// Phase 21에서 고친 버그의 회귀 방지 테스트 — verifyPassword는 로그인과 달리 세션에 영향을 주면 안 된다.
		Member member = localMemberWithPassword();
		when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

		authService.verifyPassword(member.getId(), RAW_PASSWORD);

		verify(refreshTokenStore, never()).save(any(), any(), anyLong());
	}

	@Test
	void refresh_저장된_토큰과_다르면_거부() {
		Member member = localMemberWithPassword();
		String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());
		when(refreshTokenStore.matches(member.getId(), refreshToken)).thenReturn(false);

		assertThatThrownBy(() -> authService.refresh(refreshToken))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void refresh_access_token으로는_갱신_불가() {
		Member member = localMemberWithPassword();
		String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole());

		assertThatThrownBy(() -> authService.refresh(accessToken))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void refresh_유효하면_새_토큰_쌍을_발급() {
		Member member = localMemberWithPassword();
		String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());
		when(refreshTokenStore.matches(member.getId(), refreshToken)).thenReturn(true);
		when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

		AuthService.TokenIssueResult result = authService.refresh(refreshToken);

		assertThat(result.accessToken()).isNotBlank();
		assertThat(result.refreshToken()).isNotBlank();
	}

	@Test
	void refresh_제재된_회원은_거부() {
		Member member = localMemberWithPassword();
		String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());
		when(refreshTokenStore.matches(member.getId(), refreshToken)).thenReturn(true);
		when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
		member.banPermanently();

		assertThatThrownBy(() -> authService.refresh(refreshToken)).isInstanceOf(ForbiddenException.class);
	}

	@Test
	void logout_refreshTokenStore를_무효화() {
		UUID memberId = UUID.randomUUID();

		authService.logout(memberId);

		verify(refreshTokenStore).invalidate(memberId);
	}
}
