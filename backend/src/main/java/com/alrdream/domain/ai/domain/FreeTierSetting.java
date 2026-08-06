package com.alrdream.domain.ai.domain;

import com.alrdream.global.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * [03] §2-1 {@code free_tier_settings} — FREE 플랜 월별 AI 생성 횟수 한도를 Admin이 조정할 수 있도록 담은
 * 단일 행 테이블. 마이그레이션이 이전 {@code app.ai.free-tier-monthly-limit} 고정값을 초기 행으로 이관해뒀다
 * ({@link com.alrdream.domain.ai.application.UsageQuotaService}는 더 이상 이 값을 하드코딩으로 읽지 않는다).
 */
@Getter
@Entity
@Table(name = "free_tier_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FreeTierSetting extends BaseEntity {

	@Id
	private UUID id;

	@Column(name = "monthly_limit", nullable = false)
	private int monthlyLimit;

	public void changeMonthlyLimit(int monthlyLimit) {
		this.monthlyLimit = monthlyLimit;
	}
}
