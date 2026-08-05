package com.alrdream.domain.ai.domain;

import com.alrdream.global.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

/** [01] 13번, [03] §5 {@code usage_quotas} — FREE 플랜의 월별 AI 생성 횟수 제한. */
@Getter
@Entity
@Table(name = "usage_quotas")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsageQuota extends BaseEntity {

	@Id
	@GeneratedValue
	@UuidGenerator
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	/** "YYYY-MM" 형식. */
	@Column(nullable = false)
	private String period;

	@Column(name = "generation_count", nullable = false)
	private int generationCount;

	@Column(name = "limit_count", nullable = false)
	private int limitCount;

	private UsageQuota(UUID userId, String period, int limitCount) {
		this.userId = userId;
		this.period = period;
		this.generationCount = 0;
		this.limitCount = limitCount;
	}

	public static UsageQuota create(UUID userId, String period, int limitCount) {
		return new UsageQuota(userId, period, limitCount);
	}

	public boolean isExceeded() {
		return generationCount >= limitCount;
	}

	public void increment() {
		this.generationCount++;
	}
}
