package com.alrdream.domain.member.domain;

import com.alrdream.global.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
}
