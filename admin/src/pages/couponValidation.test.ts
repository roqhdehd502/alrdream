import { describe, expect, it } from "vitest";
import { validateCouponForm } from "./couponValidation";

const validInput = {
  code: "SUMMER2026",
  benefitDays: "7",
  maxRedemptions: "",
  expiresAt: "",
};

describe("validateCouponForm", () => {
  it("모든 값이 유효하면 통과하고 파싱된 값을 반환한다", () => {
    const result = validateCouponForm(validInput);

    expect(result.ok).toBe(true);
    if (result.ok) {
      expect(result.code).toBe("SUMMER2026");
      expect(result.benefitDays).toBe(7);
      expect(result.maxRedemptions).toBeUndefined();
    }
  });

  it("코드가 비어있으면 거부한다", () => {
    const result = validateCouponForm({ ...validInput, code: "   " });

    expect(result).toEqual({ ok: false, message: "쿠폰 코드를 입력해주세요." });
  });

  it("지급 일수가 정수가 아니면 거부한다", () => {
    const result = validateCouponForm({ ...validInput, benefitDays: "1.5" });

    expect(result.ok).toBe(false);
  });

  it("지급 일수가 0이면 거부한다", () => {
    const result = validateCouponForm({ ...validInput, benefitDays: "0" });

    expect(result.ok).toBe(false);
  });

  it("최대 사용 횟수를 비워두면 무제한(undefined)으로 통과한다", () => {
    const result = validateCouponForm({ ...validInput, maxRedemptions: "" });

    expect(result.ok).toBe(true);
    if (result.ok) expect(result.maxRedemptions).toBeUndefined();
  });

  it("최대 사용 횟수가 음수면 거부한다 (Phase 21에서 고친 버그의 회귀 방지)", () => {
    const result = validateCouponForm({ ...validInput, maxRedemptions: "-5" });

    expect(result).toEqual({ ok: false, message: "최대 사용 횟수는 1 이상의 정수를 입력해주세요." });
  });

  it("최대 사용 횟수가 소수면 거부한다", () => {
    const result = validateCouponForm({ ...validInput, maxRedemptions: "1.5" });

    expect(result.ok).toBe(false);
  });

  it("최대 사용 횟수가 1 이상 정수면 통과한다", () => {
    const result = validateCouponForm({ ...validInput, maxRedemptions: "10" });

    expect(result.ok).toBe(true);
    if (result.ok) expect(result.maxRedemptions).toBe(10);
  });

  it("사용 기한을 비워두면 무기한으로 통과한다", () => {
    const result = validateCouponForm({ ...validInput, expiresAt: "" });

    expect(result.ok).toBe(true);
  });

  it("사용 기한이 과거면 거부한다 (Phase 21에서 고친 버그의 회귀 방지)", () => {
    const past = new Date(Date.now() - 60_000).toISOString().slice(0, 16);
    const result = validateCouponForm({ ...validInput, expiresAt: past });

    expect(result).toEqual({ ok: false, message: "코드 사용 기한은 현재보다 미래여야 합니다." });
  });

  it("사용 기한이 미래면 통과한다", () => {
    const future = new Date(Date.now() + 86_400_000).toISOString().slice(0, 16);
    const result = validateCouponForm({ ...validInput, expiresAt: future });

    expect(result.ok).toBe(true);
  });
});
