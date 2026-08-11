import { useCallback, useEffect, useState } from "react";
import { Platform, Text, View } from "react-native";
import { useRouter } from "expo-router";
import { useAuth } from "../../../auth/AuthContext";
import { subscriptionApi } from "../../../api/subscription";
import { ApiError } from "../../../api/client";
import { Button } from "../../../components/ui/Button";
import { Card } from "../../../components/ui/Card";
import { ErrorBanner, Loading } from "../../../components/ui/Feedback";
import { ScreenContainer } from "../../../components/ui/ScreenContainer";
import { useTheme, useThemedStyles } from "../../../components/ui/ThemeContext";
import { CheckIcon } from "../../../components/ui/icons";
import { SubscriptionStatusBadge } from "../../../components/subscription/SubscriptionStatusBadge";
import type { PricingResponse, SubscriptionResponse } from "../../../types";

function formatDateTime(value: string | null) {
  return value ? new Date(value).toLocaleString("ko-KR") : "-";
}

function formatDate(value: string) {
  return new Date(value).toLocaleDateString("ko-KR");
}

// 실제로 구현된 Pro 전용 혜택만 소개한다 — [01] 13번 BM이 언급하는 "고급 분석"은 아직 별도 기능으로
// 구현돼 있지 않아 여기 포함하지 않는다(혜택 과장 방지).
const PRO_BENEFITS = ["AI 생성 횟수 무제한", "설계 문서 PDF 다운로드"];

export default function SubscriptionScreen() {
  const router = useRouter();
  const { member, refreshMember } = useAuth();
  const { typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    wrap: { gap: 24 },
    section: { gap: 12 },
    planHeader: {
      flexDirection: "row" as const,
      alignItems: "center" as const,
      justifyContent: "space-between" as const,
    },
    confirmButtons: { flexDirection: "row" as const, gap: 10 },
    benefitRow: {
      flexDirection: "row" as const,
      alignItems: "center" as const,
      gap: 8,
    },
    benefitCheck: {
      width: 18,
      height: 18,
      borderRadius: 9,
      backgroundColor: colors.primary,
      alignItems: "center" as const,
      justifyContent: "center" as const,
    },
    priceRow: {
      flexDirection: "row" as const,
      alignItems: "baseline" as const,
      gap: 8,
    },
    priceStrike: {
      ...typography.muted,
      textDecorationLine: "line-through" as const,
    },
  }));

  const [subscription, setSubscription] = useState<
    SubscriptionResponse | null | undefined
  >(undefined);
  const [pricing, setPricing] = useState<PricingResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [confirmingCancel, setConfirmingCancel] = useState(false);

  // 구독 이력이 아예 없는 경우(가입 이후 한 번도 구독한 적 없음)는 정상적인 상태라 에러로 취급하지 않는다.
  const load = useCallback(async () => {
    try {
      setSubscription(await subscriptionApi.getCurrent());
    } catch {
      setSubscription(null);
    }
    subscriptionApi
      .getPricing()
      .then(setPricing)
      .catch(() => setPricing(null));
  }, []);

  useEffect(() => {
    const run = async () => {
      try {
        setSubscription(await subscriptionApi.getCurrent());
      } catch {
        setSubscription(null);
      }
      subscriptionApi
        .getPricing()
        .then(setPricing)
        .catch(() => setPricing(null));
    };
    run();
  }, []);

  const handleSubscribe = async () => {
    if (!member) return;
    setBusy(true);
    setError(null);
    try {
      const { issueBillingKey } = await import("../../../api/portone");
      const billingKeyId = await issueBillingKey(member.email);
      setSubscription(await subscriptionApi.subscribe(billingKeyId));
      load();
      refreshMember();
    } catch (e) {
      setError(
        e instanceof ApiError
          ? e.message
          : e instanceof Error
            ? e.message
            : "구독에 실패했습니다.",
      );
    } finally {
      setBusy(false);
    }
  };

  const handleCancel = async () => {
    setBusy(true);
    setError(null);
    try {
      setSubscription(await subscriptionApi.cancel());
      setConfirmingCancel(false);
      load();
      refreshMember();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "해지에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  // 결제 구독(subscription.status) 유무와 별개로, 쿠폰/관리자 지급만으로도 Pro일 수 있다 — member.plan이
  // 항상 옳은 신호이고, subscription은 "관리할 결제 구독이 있는지"만 알려준다(Phase 19 쿠폰 도입 이전에는
  // Pro가 항상 결제 구독에서만 나와 이 둘이 같았지만, 이제는 구분해야 한다).
  const isPro = member?.plan === "PRO";
  const hasActiveSubscription =
    subscription?.status === "ACTIVE" || subscription?.status === "PAST_DUE";

  return (
    <ScreenContainer>
      <View style={styles.wrap}>
        <View style={styles.section}>
          {subscription === undefined ? (
            <Loading />
          ) : (
            <Card style={styles.section}>
              <View style={styles.planHeader}>
                <Text style={typography.heading}>
                  {isPro ? "Pro 플랜" : "Free 플랜"}
                </Text>
                {subscription && (
                  <SubscriptionStatusBadge status={subscription.status} />
                )}
              </View>

              {isPro && !hasActiveSubscription && member?.proExpiresAt && (
                <Text style={typography.muted}>
                  쿠폰/프로모션으로 {formatDate(member.proExpiresAt)}까지 Pro를
                  이용할 수 있어요.
                </Text>
              )}

              {hasActiveSubscription && subscription ? (
                <>
                  <Text style={typography.muted}>
                    {subscription.status === "PAST_DUE"
                      ? "결제 승인을 확인하고 있어요. 잠시 후 다시 확인해주세요."
                      : `다음 결제일: ${formatDateTime(subscription.nextBillingAt)}`}
                  </Text>
                  <ErrorBanner message={error} />
                  {!confirmingCancel ? (
                    <Button
                      label="구독 해지"
                      variant="ghost"
                      onPress={() => setConfirmingCancel(true)}
                      style={{ alignSelf: "flex-start" }}
                    />
                  ) : (
                    <Card tone="danger" style={styles.confirmButtons}>
                      <Text style={typography.muted}>
                        지금 해지하면 즉시 Free로 전환되며, 남은 기간에 대한
                        환불은 없습니다.
                      </Text>
                      <View style={styles.confirmButtons}>
                        <Button
                          label="취소"
                          variant="secondary"
                          onPress={() => setConfirmingCancel(false)}
                        />
                        <Button
                          label="해지하기"
                          variant="danger"
                          onPress={handleCancel}
                          loading={busy}
                        />
                      </View>
                    </Card>
                  )}
                </>
              ) : (
                <>
                  <View style={styles.section}>
                    {PRO_BENEFITS.map((benefit) => (
                      <View key={benefit} style={styles.benefitRow}>
                        <View style={styles.benefitCheck}>
                          <CheckIcon size={11} color="#fff" />
                        </View>
                        <Text style={typography.body}>{benefit}</Text>
                      </View>
                    ))}
                  </View>

                  {pricing && (
                    <View>
                      <View style={styles.priceRow}>
                        {pricing.promoPriceKrw !== null ? (
                          <>
                            <Text style={styles.priceStrike}>
                              {pricing.basePriceKrw.toLocaleString("ko-KR")}원
                            </Text>
                            <Text style={typography.title}>
                              {pricing.promoPriceKrw.toLocaleString("ko-KR")}원
                            </Text>
                            <Text style={typography.muted}>/ 월</Text>
                          </>
                        ) : (
                          <>
                            <Text style={typography.title}>
                              {pricing.basePriceKrw.toLocaleString("ko-KR")}원
                            </Text>
                            <Text style={typography.muted}>/ 월</Text>
                          </>
                        )}
                      </View>
                      {pricing.promoEndsAt && (
                        <Text style={typography.muted}>
                          프로모션가 — {formatDate(pricing.promoEndsAt)}까지
                        </Text>
                      )}
                    </View>
                  )}

                  <ErrorBanner message={error} />
                  {Platform.OS === "web" ? (
                    <Button
                      label="Pro 구독하기"
                      onPress={handleSubscribe}
                      loading={busy}
                      style={{ alignSelf: "flex-start" }}
                    />
                  ) : (
                    <Text style={typography.muted}>
                      Pro 구독은 웹 버전(alrdream 웹사이트)에서 신청할 수
                      있어요.
                    </Text>
                  )}
                </>
              )}
            </Card>
          )}
        </View>

        <Button
          label="결제 내역 보기"
          variant="secondary"
          onPress={() => router.push("/subscription/payments")}
          style={{ alignSelf: "flex-start" }}
        />
      </View>
    </ScreenContainer>
  );
}
