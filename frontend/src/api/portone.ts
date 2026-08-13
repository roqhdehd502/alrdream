/**
 * [03] §4-7 — PortOne 빌링키 발급(웹 전용). `@portone/browser-sdk`는 브라우저 전용 SDK라 동적 import로
 * 감싸둔다 — 호출부가 `Platform.OS === "web"`일 때만 이 모듈을 불러오므로 네이티브 번들에는 영향이 없다
 * (§4-5 Google 로그인의 웹 전용 게이팅과 동일한 이유).
 */
export async function issueBillingKey(customerEmail: string): Promise<string> {
  const storeId = process.env.EXPO_PUBLIC_PORTONE_STORE_ID;
  const channelKey = process.env.EXPO_PUBLIC_PORTONE_CHANNEL_KEY;
  if (!storeId || !channelKey) {
    throw new Error("결제 설정이 아직 완료되지 않았습니다. 잠시 후 다시 시도해주세요.");
  }

  const PortOne = await import("@portone/browser-sdk/v2");
  const response = await PortOne.requestIssueBillingKey({
    storeId,
    channelKey,
    billingKeyMethod: "CARD",
    issueId: `bk_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`,
    issueName: "알려드림 Pro 구독",
    customer: { email: customerEmail },
  });

  if (!response || response.code) {
    throw new Error(response?.message ?? "카드 등록에 실패했습니다.");
  }
  return response.billingKey;
}
