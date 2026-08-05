package com.alrdream.domain.workspace.domain;

import com.alrdream.global.jpa.SoftDeleteBaseEntity;
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

/** [03] §5 {@code workspaces} 테이블. 소유자(user)별로 격리되어 조회/수정/삭제된다. */
@Getter
@Entity
@Table(name = "workspaces")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workspace extends SoftDeleteBaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private WorkspaceStatus status;

	private Workspace(UUID userId, String name) {
		this.userId = userId;
		this.name = name;
		this.status = WorkspaceStatus.ACTIVE;
	}

	public static Workspace create(UUID userId, String name) {
		return new Workspace(userId, name);
	}

	public void rename(String name) {
		this.name = name;
	}
}
