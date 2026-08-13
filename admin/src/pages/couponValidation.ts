export interface CouponFormInput {
  code: string;
  benefitDays: string;
  maxRedemptions: string;
  expiresAt: string;
}

export interface CouponFormValid {
  ok: true;
  code: string;
  benefitDays: number;
  maxRedemptions: number | undefined;
  expiresAt: string;
}

export interface CouponFormInvalid {
  ok: false;
  message: string;
}

/**
 * `CouponsPage`의 쿠폰 생성 폼 검증을 UI에서 분리한 순수 함수 — 단위 테스트 가능하게 하려고 추출했다
 * (Phase 21 전수 점검에서 이 폼의 maxRedemptions/expiresAt 검증 누락을 발견해 고친 로직이 다시 깨지지
 * 않도록 회귀 테스트를 붙이는 것이 목적).
 */
export function validateCouponForm(input: CouponFormInput): CouponFormValid | CouponFormInvalid {
  if (!input.code.trim()) {
    return { ok: false, message: "쿠폰 코드를 입력해주세요." };
  }

  const benefitDays = Number(input.benefitDays);
  if (!Number.isInteger(benefitDays) || benefitDays < 1) {
    return { ok: false, message: "지급 일수는 1 이상의 정수를 입력해주세요." };
  }

  let maxRedemptions: number | undefined;
  if (input.maxRedemptions.trim()) {
    maxRedemptions = Number(input.maxRedemptions);
    if (!Number.isInteger(maxRedemptions) || maxRedemptions < 1) {
      return { ok: false, message: "최대 사용 횟수는 1 이상의 정수를 입력해주세요." };
    }
  }

  if (input.expiresAt && new Date(input.expiresAt).getTime() <= Date.now()) {
    return { ok: false, message: "코드 사용 기한은 현재보다 미래여야 합니다." };
  }

  return { ok: true, code: input.code.trim(), benefitDays, maxRedemptions, expiresAt: input.expiresAt };
}
