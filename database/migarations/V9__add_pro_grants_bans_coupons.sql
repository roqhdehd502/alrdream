-- [04_milestone.md] Phase 19 — 쿠폰/관리자 지급으로 "구독과 무관하게 이 시각까지는 Pro"를 보장하는
-- pro_expires_at, 계정 제재(일시/영구), 쿠폰 도메인 2개 테이블을 추가한다.
--
-- pro_expires_at: 결제 웹훅/사용자 해지가 무조건 plan=FREE로 되돌리던 기존 로직이 이 값을 함부로
-- 덮어쓰지 않도록 애플리케이션(Member#syncPlanFromSubscriptionEnd)이 지켜 쓴다 — 이 값이 아직 미래면
-- FREE로 내리지 않는다.
ALTER TABLE users ADD COLUMN pro_expires_at TIMESTAMPTZ;

-- 제재: 영구 정지(permanent_ban)와 기간제 정지(temp_ban_until)를 분리해 관리한다. sentinel 날짜(예:
-- "9999-12-31"로 영구 표현) 방식 대신 명시적 boolean을 쓴 이유는 "영구인지 아닌지"를 코드에서 바로
-- 읽히게 하기 위함 — 유효 제재 여부는 permanent_ban OR (temp_ban_until IS NOT NULL AND temp_ban_until > now()).
ALTER TABLE users ADD COLUMN temp_ban_until TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN permanent_ban BOOLEAN NOT NULL DEFAULT false;

-- =========================================================
-- coupons — 이벤트로 외부 공개하는 쿠폰 코드. 코드는 자동생성이 아니라 관리자가 원하는 문자열을 직접
-- 지정한다(마케팅이 미리 공지하는 코드라서).
-- =========================================================
CREATE TABLE coupons (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code               VARCHAR(64) NOT NULL UNIQUE,
    benefit_days       INT         NOT NULL CHECK (benefit_days > 0),
    max_redemptions    INT,                          -- NULL = 무제한
    redemption_count   INT         NOT NULL DEFAULT 0,
    expires_at         TIMESTAMPTZ,                  -- NULL = 코드 자체의 사용 기한 없음
    active             BOOLEAN     NOT NULL DEFAULT true,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE coupons ENABLE ROW LEVEL SECURITY;

-- =========================================================
-- coupon_redemptions — 사용자별 쿠폰 사용 이력. UNIQUE(coupon_id, user_id)로 동일 유저가 같은 코드를
-- 두 번 쓰는 것을 DB 레벨에서 막는다(여러 사용자가 같은 코드를 각자 한 번씩 쓰는 건 당연히 허용).
-- =========================================================
CREATE TABLE coupon_redemptions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coupon_id      UUID        NOT NULL REFERENCES coupons(id),
    user_id        UUID        NOT NULL REFERENCES users(id),
    redeemed_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    granted_until  TIMESTAMPTZ NOT NULL,
    UNIQUE (coupon_id, user_id)
);

ALTER TABLE coupon_redemptions ENABLE ROW LEVEL SECURITY;
