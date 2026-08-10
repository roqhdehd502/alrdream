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
