-- [04_milestone.md] Phase 18 후속 — Admin의 "정기 구독 상품 가격/프로모션 관리" 기능을 위한 테이블.
-- free_tier_settings(V5)와 동일한 패턴(단일 행, Admin이 조회/갱신)이다. 이전까지 Pro 월 구독료는
-- application.yml의 app.portone.pro-monthly-price-krw 고정값이었던 것을 여기로 옮긴다 — 값 변경/프로모션
-- 적용에 재배포가 필요 없게 하기 위함.
--
-- promo_* 세 컬럼은 "전부 NULL(프로모션 없음)" 또는 "전부 채워짐(프로모션 진행 중)" 둘 중 하나여야 한다
-- (SubscriptionPricing 엔티티가 이 불변식을 지켜 쓴다). 현재 유효가는 애플리케이션이
-- now() ∈ [promo_starts_at, promo_ends_at) 이고 promo_price_krw가 있으면 promo_price_krw, 아니면
-- base_price_krw로 계산한다 — 신규 구독/갱신(재예약) 모두 이 값을 그대로 청구 금액으로 쓴다(가입 시점
-- 가격을 별도로 잠그지 않음 — 04_milestone.md 설계 결정 참고).
CREATE TABLE subscription_pricing (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    base_price_krw   BIGINT      NOT NULL,
    promo_price_krw  BIGINT,
    promo_starts_at  TIMESTAMPTZ,
    promo_ends_at    TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (base_price_krw >= 0),
    CHECK (promo_price_krw IS NULL OR promo_price_krw >= 0),
    CHECK (
        (promo_price_krw IS NULL AND promo_starts_at IS NULL AND promo_ends_at IS NULL)
        OR (promo_price_krw IS NOT NULL AND promo_starts_at IS NOT NULL AND promo_ends_at IS NOT NULL
            AND promo_ends_at > promo_starts_at)
    )
);

ALTER TABLE subscription_pricing ENABLE ROW LEVEL SECURITY;

-- 기존 application.yml의 app.portone.pro-monthly-price-krw(9900) 기본값을 그대로 이관.
INSERT INTO subscription_pricing (base_price_krw) VALUES (9900);
