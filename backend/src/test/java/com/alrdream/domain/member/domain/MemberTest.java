package com.alrdream.domain.member.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/**
 * 순수 도메인 로직 단위 테스트 — Spring 컨텍스트/DB 없이 {@link Member}의 비즈니스 규칙만 검증한다.
 */
class MemberTest {

	@Test
	void createLocal_LOCAL_provider로_FREE_USER_생성() {
		Member member = Member.createLocal("user@example.com", "hashed");

		assertThat(member.getEmail()).isEqualTo("user@example.com");
		assertThat(member.getPasswordHash()).isEqualTo("hashed");
		assertThat(member.getProvider()).isEqualTo(AuthProvider.LOCAL);
		assertThat(member.getRole()).isEqualTo(MemberRole.USER);
		assertThat(member.getPlan()).isEqualTo(MemberPlan.FREE);
	}

	@Test
	void createOAuth_비밀번호_없이_생성() {
		Member member = Member.createOAuth("user@example.com", AuthProvider.GOOGLE, "google-sub-1");

		assertThat(member.getPasswordHash()).isNull();
		assertThat(member.getProvider()).isEqualTo(AuthProvider.GOOGLE);
		assertThat(member.getProviderId()).isEqualTo("google-sub-1");
	}

	@Test
	void createLocal_이메일_미인증_상태로_시작() {
		Member member = Member.createLocal("user@example.com", "hashed");

		assertThat(member.isEmailVerified()).isFalse();
	}

	@Test
	void createOAuth_가입_즉시_이메일_인증됨() {
		Member member = Member.createOAuth("user@example.com", AuthProvider.GOOGLE, "google-sub-1");

		assertThat(member.isEmailVerified()).isTrue();
	}

	@Test
	void markEmailVerified_인증_상태로_전환() {
		Member member = Member.createLocal("user@example.com", "hashed");

		member.markEmailVerified();

		assertThat(member.isEmailVerified()).isTrue();
	}

	@Test
	void extendProUntil_FREE에서_처음_지급받으면_오늘부터_기산() {
		Member member = Member.createLocal("user@example.com", "hashed");
		OffsetDateTime before = OffsetDateTime.now();

		member.extendProUntil(7);

		assertThat(member.getPlan()).isEqualTo(MemberPlan.PRO);
		assertThat(member.getProExpiresAt()).isAfter(before.plusDays(6)).isBefore(before.plusDays(8));
	}

	@Test
	void extendProUntil_이미_유효한_Pro면_기존_만료일부터_이어서_연장() {
		Member member = Member.createLocal("user@example.com", "hashed");
		member.extendProUntil(10); // 기존에 10일 지급됨
		OffsetDateTime firstExpiry = member.getProExpiresAt();

		member.extendProUntil(5); // 쿠폰 하나 더 등록 — 남은 기간 위에 이어 붙어야 함(오늘부터 5일이 아님)

		assertThat(member.getProExpiresAt()).isEqualTo(firstExpiry.plusDays(5));
	}

	@Test
	void extendProUntil_만료된_Pro는_오늘부터_새로_기산() {
		Member member = Member.createLocal("user@example.com", "hashed");
		member.extendProUntil(-1); // 이미 어제 만료된 상태를 시뮬레이션
		OffsetDateTime expiredAt = member.getProExpiresAt();
		OffsetDateTime before = OffsetDateTime.now();

		member.extendProUntil(3);

		assertThat(member.getProExpiresAt()).isAfter(expiredAt); // 만료 시점 기준이 아니라
		assertThat(member.getProExpiresAt()).isAfter(before.plusDays(2)).isBefore(before.plusDays(4)); // 오늘부터 3일
	}

	@Test
	void syncPlanFromSubscriptionEnd_proExpiresAt이_아직_유효하면_FREE로_내리지_않음() {
		Member member = Member.createLocal("user@example.com", "hashed");
		member.extendProUntil(30); // 쿠폰으로 30일 보장된 상태

		member.syncPlanFromSubscriptionEnd(); // 결제 해지 웹훅 등으로 호출됨

		assertThat(member.getPlan()).isEqualTo(MemberPlan.PRO); // 쿠폰 보장 기간이 남아있어 유지
	}

	@Test
	void syncPlanFromSubscriptionEnd_proExpiresAt이_없으면_FREE로_전환() {
		Member member = Member.createLocal("user@example.com", "hashed");
		member.changePlan(MemberPlan.PRO); // proExpiresAt 없이 순수 구독으로만 PRO였던 상태

		member.syncPlanFromSubscriptionEnd();

		assertThat(member.getPlan()).isEqualTo(MemberPlan.FREE);
	}

	@Test
	void clearProGrant_쿠폰_보장_기간이_남아있어도_무조건_FREE로_전환() {
		Member member = Member.createLocal("user@example.com", "hashed");
		member.extendProUntil(30);

		member.clearProGrant(); // 관리자 강제 Free 전환

		assertThat(member.getPlan()).isEqualTo(MemberPlan.FREE);
		assertThat(member.getProExpiresAt()).isNull();
	}

	@Test
	void isBanned_영구_제재는_해제_전까지_true() {
		Member member = Member.createLocal("user@example.com", "hashed");
		member.banPermanently();

		assertThat(member.isBanned()).isTrue();
	}

	@Test
	void isBanned_일시_제재는_해제_시각_이후_false() {
		Member member = Member.createLocal("user@example.com", "hashed");
		member.banTemporarily(OffsetDateTime.now().minusMinutes(1)); // 이미 지난 해제 시각

		assertThat(member.isBanned()).isFalse();
	}

	@Test
	void isBanned_일시_제재는_해제_시각_전까지_true() {
		Member member = Member.createLocal("user@example.com", "hashed");
		member.banTemporarily(OffsetDateTime.now().plusHours(1));

		assertThat(member.isBanned()).isTrue();
	}

	@Test
	void unban_영구_일시_제재_둘_다_해제() {
		Member member = Member.createLocal("user@example.com", "hashed");
		member.banPermanently();

		member.unban();

		assertThat(member.isBanned()).isFalse();
	}

	@Test
	void banMessage_영구와_일시_문구가_다름() {
		Member permanent = Member.createLocal("a@example.com", "hashed");
		permanent.banPermanently();
		Member temporary = Member.createLocal("b@example.com", "hashed");
		temporary.banTemporarily(OffsetDateTime.now().plusDays(1));

		assertThat(permanent.banMessage()).contains("영구");
		assertThat(temporary.banMessage()).contains("해제 예정");
	}

	@Test
	void changeName_공백만_있으면_null로_정규화() {
		Member member = Member.createLocal("user@example.com", "hashed");

		member.changeName("   ");

		assertThat(member.getName()).isNull();
	}

	@Test
	void changeName_앞뒤_공백은_trim() {
		Member member = Member.createLocal("user@example.com", "hashed");

		member.changeName("  홍길동  ");

		assertThat(member.getName()).isEqualTo("홍길동");
	}

	@Test
	void withdraw_이메일과_비밀번호를_익명화하고_withdrawnAt을_기록() {
		Member member = Member.createLocal("user@example.com", "hashed");

		member.withdraw();

		assertThat(member.getEmail()).startsWith("withdrawn-").endsWith("@deleted.local");
		assertThat(member.getPasswordHash()).isNull();
		assertThat(member.getProviderId()).isNull();
		assertThat(member.isWithdrawn()).isTrue();
	}

	@Test
	void isWithdrawn_탈퇴_전에는_false() {
		Member member = Member.createLocal("user@example.com", "hashed");

		assertThat(member.isWithdrawn()).isFalse();
	}
}
