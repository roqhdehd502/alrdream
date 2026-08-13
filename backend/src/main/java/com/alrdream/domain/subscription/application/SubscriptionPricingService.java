package com.alrdream.domain.subscription.application;

import com.alrdream.domain.subscription.domain.SubscriptionPricing;
import com.alrdream.domain.subscription.domain.SubscriptionPricingRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [03] §2-1 Admin의 "Pro 구독 가격/프로모션 관리". {@code subscription_pricing}은 마이그레이션(V8)이 이미
 * 초기 행을 넣어둔 단일 행 테이블이라, 애플리케이션은 그 행을 읽고 갱신만 한다(새로 만들지 않음).
 */
@Service
@Transactional(readOnly = true)
public class SubscriptionPricingService {

	private final SubscriptionPricingRepository subscriptionPricingRepository;

	public SubscriptionPricingService(SubscriptionPricingRepository subscriptionPricingRepository) {
		this.subscriptionPricingRepository = subscriptionPricingRepository;
	}

	public SubscriptionPricing get() {
		List<SubscriptionPricing> rows = subscriptionPricingRepository.findAllByOrderByCreatedAtAsc();
		if (rows.isEmpty()) {
			throw new IllegalStateException("subscription_pricing 초기 행이 없습니다 — 마이그레이션이 정상적으로 실행됐는지 확인하세요.");
		}
		return rows.get(0);
	}

	/** 결제 요청 시점의 청구 금액 — 신규 구독/기존 구독 갱신(재예약) 모두 이 값을 그대로 쓴다. */
	public long getEffectivePriceKrw() {
		return get().effectivePriceKrw(OffsetDateTime.now());
	}

	@Transactional
	public SubscriptionPricing updateBasePrice(long basePriceKrw) {
		SubscriptionPricing pricing = get();
		pricing.changeBasePrice(basePriceKrw);
		return pricing;
	}

	@Transactional
	public SubscriptionPricing setPromotion(long promoPriceKrw, OffsetDateTime promoStartsAt, OffsetDateTime promoEndsAt) {
		SubscriptionPricing pricing = get();
		pricing.setPromotion(promoPriceKrw, promoStartsAt, promoEndsAt);
		return pricing;
	}

	@Transactional
	public SubscriptionPricing clearPromotion() {
		SubscriptionPricing pricing = get();
		pricing.clearPromotion();
		return pricing;
	}
}
