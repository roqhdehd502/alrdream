package com.alrdream.domain.ai.application;

import com.alrdream.domain.ai.domain.FreeTierSetting;
import com.alrdream.domain.ai.domain.FreeTierSettingRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [03] §2-1 Admin의 "Free 티어 생성 횟수 한도 조정". {@code free_tier_settings}는 마이그레이션(V5)이 이미
 * 초기 행을 넣어둔 단일 행 테이블이라, 애플리케이션은 그 행을 읽고 갱신만 한다(새로 만들지 않음).
 */
@Service
@Transactional(readOnly = true)
public class FreeTierSettingService {

	private final FreeTierSettingRepository freeTierSettingRepository;

	public FreeTierSettingService(FreeTierSettingRepository freeTierSettingRepository) {
		this.freeTierSettingRepository = freeTierSettingRepository;
	}

	public FreeTierSetting get() {
		List<FreeTierSetting> settings = freeTierSettingRepository.findAllByOrderByCreatedAtAsc();
		if (settings.isEmpty()) {
			throw new IllegalStateException("free_tier_settings 초기 행이 없습니다 — 마이그레이션이 정상적으로 실행됐는지 확인하세요.");
		}
		return settings.get(0);
	}

	@Transactional
	public FreeTierSetting updateMonthlyLimit(int monthlyLimit) {
		FreeTierSetting setting = get();
		setting.changeMonthlyLimit(monthlyLimit);
		return setting;
	}
}
