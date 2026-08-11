package com.alrdream.domain.member.domain;

import com.alrdream.global.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/** [03] §5 {@code users} 테이블. Spring Security의 {@code User} 클래스와 이름이 겹치지 않도록 {@code Member}로 명명. */
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash")
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AuthProvider provider;

	@Column(name = "provider_id")
	private String providerId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MemberRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MemberPlan plan;

	/** Phase 16 — 회원 탈퇴. NULL이 아니면 탈퇴 상태(로그인 차단, AuthService에서 검사). */
	@Column(name = "withdrawn_at")
	private OffsetDateTime withdrawnAt;

	/**
	 * Phase 19 — 쿠폰/관리자 지급으로 구독과 무관하게 "이 시각까지는 Pro"를 보장하는 값. 결제 웹훅/사용자
	 * 해지가 이 값을 무시하고 무조건 FREE로 되돌리지 않도록 {@link #syncPlanFromSubscriptionEnd()}가 지켜서
	 * 쓴다. 결제 구독이 살아있는 동안은 보통 NULL(구독 자체가 Pro를 보장하므로 별도 만료선이 필요 없음).
	 */
	@Column(name = "pro_expires_at")
	private OffsetDateTime proExpiresAt;

	/** Phase 19 — 기간제 제재. NULL이면 제재 없음, 미래 시각이면 그때까지 제재, 과거 시각이면 만료된 제재(무시). */
	@Column(name = "temp_ban_until")
	private OffsetDateTime tempBanUntil;

	/** Phase 19 — 영구 제재. */
	@Column(name = "permanent_ban", nullable = false)
	private boolean permanentBan;

	private Member(String email, String passwordHash, AuthProvider provider, String providerId) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.provider = provider;
		this.providerId = providerId;
		this.role = MemberRole.USER;
		this.plan = MemberPlan.FREE;
	}

	public static Member createLocal(String email, String passwordHash) {
		return new Member(email, passwordHash, AuthProvider.LOCAL, null);
	}

	public static Member createOAuth(String email, AuthProvider provider, String providerId) {
		return new Member(email, null, provider, providerId);
	}

	/** [03] §4-7 — 구독 결제 성공/실패 웹훅에 따라 Pro 권한을 반영한다. */
	public void changePlan(MemberPlan plan) {
		this.plan = plan;
	}

	/**
	 * Phase 19 — 쿠폰 사용/관리자 지급 공용 로직. FREE였다면 오늘부터 {@code days}일 Pro를 부여하고, 이미
	 * {@link #proExpiresAt}이 유효한 상태였다면(기존 지급이 남아있거나 이번에 처음 지급받는 게 아니라면) 그
	 * 시점부터 이어서 연장한다 — "Free는 신규 지급, 이미 Pro면 갱신 기간을 혜택만큼 늘려준다"는 요구사항을
	 * 하나의 규칙(연장 시작점 = max(기존 proExpiresAt, now))으로 구현한다.
	 */
	public void extendProUntil(int days) {
		OffsetDateTime now = OffsetDateTime.now();
		OffsetDateTime base = (proExpiresAt != null && proExpiresAt.isAfter(now)) ? proExpiresAt : now;
		this.proExpiresAt = base.plusDays(days);
		this.plan = MemberPlan.PRO;
	}

	/**
	 * Phase 19 — 결제 해지/실패로 Pro 권한을 회수해야 할 때 쓴다. {@link #proExpiresAt}이 아직 유효하면(쿠폰
	 * 등으로 구독과 별개로 보장된 기간이 남아있으면) FREE로 내리지 않는다 — 만료 처리는
	 * {@code ProGrantExpirationScheduler}가 별도로 담당한다.
	 */
	public void syncPlanFromSubscriptionEnd() {
		OffsetDateTime now = OffsetDateTime.now();
		if (proExpiresAt != null && proExpiresAt.isAfter(now)) {
			return;
		}
		this.plan = MemberPlan.FREE;
	}

	/** Phase 19 — Admin이 강제로 Free 전환시킬 때 쓴다. 쿠폰 등으로 남아있는 보장 기간까지 무조건 지운다. */
	public void clearProGrant() {
		this.plan = MemberPlan.FREE;
		this.proExpiresAt = null;
	}

	public void banTemporarily(OffsetDateTime until) {
		this.tempBanUntil = until;
	}

	public void banPermanently() {
		this.permanentBan = true;
	}

	public void unban() {
		this.tempBanUntil = null;
		this.permanentBan = false;
	}

	public boolean isBanned() {
		return permanentBan || (tempBanUntil != null && tempBanUntil.isAfter(OffsetDateTime.now()));
	}

	/** JwtAuthenticationFilter/AuthService가 제재된 사용자에게 그대로 보여줄 메시지. */
	public String banMessage() {
		if (permanentBan) {
			return "영구적으로 이용이 제한된 계정입니다.";
		}
		return "일시적으로 이용이 제한된 계정입니다. (해제 예정: " + tempBanUntil + ")";
	}

	/** Phase 16 — 비밀번호 재설정. 호출 전에 provider가 LOCAL인지 확인하는 것은 호출부(PasswordResetService)의 책임이다. */
	public void changePassword(String newPasswordHash) {
		this.passwordHash = newPasswordHash;
	}

	public boolean isWithdrawn() {
		return withdrawnAt != null;
	}

	/**
	 * Phase 16 — 회원 탈퇴. 워크스페이스/구독/결제이력 등은 FK로 남아있어야 해(하드 삭제 불가) 이메일/외부
	 * 연동 식별자만 익명화하고 로그인은 영구히 막는다. 이메일을 비워 원래 이메일로 재가입할 수 있게 한다.
	 */
	public void withdraw() {
		this.email = "withdrawn-" + this.id + "@deleted.local";
		this.passwordHash = null;
		this.providerId = null;
		this.withdrawnAt = OffsetDateTime.now();
	}
}
