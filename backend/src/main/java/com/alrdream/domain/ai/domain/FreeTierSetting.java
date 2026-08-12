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
 * [03] §2-1 {@code free_tier_settings} — FREE/PRO 플랜의 월별 AI 생성 횟수 한도를 Admin이 조정할 수 있도록
 * 담은 단일 행 테이블. 마이그레이션이 초기 행을 넣어뒀다({@link com.alrdream.domain.ai.application.UsageQuotaService}는
 * 이 값을 하드코딩으로 읽지 않는다). 테이블/클래스 이름은 FREE 티어만 있던 시절의 이름을 그대로 쓰지만(Phase 20
 * 이전), Phase 20부터 PRO 한도도 이 행에 함께 저장한다.
 */
@Getter
@Entity
@Table(name = "free_tier_settings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FreeTierSetting extends BaseEntity {

	@Id
	private UUID id;

	@Column(name = "free_monthly_limit", nullable = false)
	private int freeMonthlyLimit;

	@Column(name = "pro_monthly_limit", nullable = false)
	private int proMonthlyLimit;

	public void changeFreeMonthlyLimit(int freeMonthlyLimit) {
		this.freeMonthlyLimit = freeMonthlyLimit;
	}

	public void changeProMonthlyLimit(int proMonthlyLimit) {
		this.proMonthlyLimit = proMonthlyLimit;
	}
}
