import { useCallback, useEffect, useState } from "react";
import { Platform, Text, View } from "react-native";
import { useRouter } from "expo-router";
import { useAuth } from "../../../auth/AuthContext";
import { subscriptionApi } from "../../../api/subscription";
import { ApiError } from "../../../api/client";
import { Badge } from "../../../components/ui/Badge";
import { Button } from "../../../components/ui/Button";
import { Card } from "../../../components/ui/Card";
import { ErrorBanner, Loading } from "../../../components/ui/Feedback";
import { IconChip } from "../../../components/ui/IconChip";
import { ScreenContainer } from "../../../components/ui/ScreenContainer";
import { useTheme, useThemedStyles } from "../../../components/ui/ThemeContext";
import { fontFamily, shadows } from "../../../components/ui/theme";
import { CheckIcon, SubscriptionIcon } from "../../../components/ui/icons";
import { SubscriptionStatusBadge } from "../../../components/subscription/SubscriptionStatusBadge";
import type { PricingResponse, SubscriptionResponse } from "../../../types";

function formatDateTime(value: string | null) {
  return value ? new Date(value).toLocaleString("ko-KR") : "-";
}

function formatDate(value: string) {
  return new Date(value).toLocaleDateString("ko-KR");
}

// 실제로 구현된 Pro 전용 혜택만 소개한다 — [01] 13번 BM이 언급하는 "고급 분석"은 아직 별도 기능으로
// 구현돼 있지 않아 여기 포함하지 않는다(혜택 과장 방지). Phase 20부터 Pro도 무제한이 아니라 월 10회 한도라
// "무제한"이라고 쓰면 안 된다 — 정확한 한도는 admin에서 바뀔 수 있어 마이페이지의 실사용량 화면이 최종
// 소스이고, 여기 숫자는 마케팅 카피용 스냅샷이다.
const PRO_BENEFITS = ["월 10회 AI 생성", "설계 문서 PDF 다운로드"];
// Pro 전용 혜택의 반대편 — 새 숫자를 지어내지 않고 PRO_BENEFITS와 대구를 이루는 사실만 적는다
// (정확한 월 한도 수치는 마이페이지의 "이번 달 AI 생성 사용량"에서 보여준다, Phase 19 설계 결정).
const FREE_LIMITATIONS = [
  "월 AI 생성 횟수 제한",
  "설계 문서 PDF 다운로드 미지원",
];

export default function SubscriptionScreen() {
  const router = useRouter();
  const { member, refreshMember } = useAuth();
  const { colors, typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    wrap: { gap: 20 },
    section: { gap: 12 },
    planCard: { gap: 14, position: "relative" as const },
    proCard: {
      backgroundColor: colors.primarySoft,
      borderColor: colors.primary,
      borderWidth: 1,
      ...shadows.md,
    },
    currentCard: { borderWidth: 2, borderColor: colors.primary },
    planHeader: {
      flexDirection: "row" as const,
      alignItems: "center" as const,
      justifyContent: "space-between" as const,
    },
    proTitleRow: {
      flexDirection: "row" as const,
      alignItems: "center" as const,
      gap: 10,
    },
    ribbon: { position: "absolute" as const, top: -12, right: 16, zIndex: 1 },
    confirmButtons: { flexDirection: "row" as const, gap: 10 },
    benefitRow: {
      flexDirection: "row" as const,
      alignItems: "center" as const,
      gap: 10,
    },
    benefitCheck: {
      width: 20,
      height: 20,
      borderRadius: 10,
      backgroundColor: colors.primary,
      alignItems: "center" as const,
      justifyContent: "center" as const,
    },
    limitMark: {
      width: 20,
      height: 20,
      borderRadius: 10,
      backgroundColor: colors.border,
      alignItems: "center" as const,
      justifyContent: "center" as const,
    },
    priceRow: {
      flexDirection: "row" as const,
      alignItems: "baseline" as const,
      gap: 8,
    },
    bigPrice: { fontSize: 32, fontFamily: fontFamily.bold, color: colors.text },
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
        {subscription === undefined ? (
          <Loading />
        ) : (
          <>
            <Card style={[styles.planCard, !isPro && styles.currentCard]}>
              <View style={styles.planHeader}>
                <Text style={typography.heading}>Free 플랜</Text>
                {!isPro && <Badge label="현재 플랜" tone="neutral" />}
              </View>

              <View style={styles.priceRow}>
                <Text style={styles.bigPrice}>0원</Text>
                <Text style={typography.muted}>/ 월</Text>
              </View>

              <View style={styles.section}>
                {FREE_LIMITATIONS.map((item) => (
                  <View key={item} style={styles.benefitRow}>
                    <View style={styles.limitMark}>
                      <CheckIcon size={11} color={colors.textMuted} />
                    </View>
                    <Text style={typography.muted}>{item}</Text>
                  </View>
                ))}
              </View>
            </Card>

            <Card
              style={[
                styles.planCard,
                styles.proCard,
                isPro && styles.currentCard,
              ]}
            >
              <View style={styles.planHeader}>
                <View style={styles.proTitleRow}>
                  <IconChip icon={<SubscriptionIcon size={16} color={colors.primary} />} size={32} tone="surface" />
                  <Text style={typography.heading}>Pro 플랜</Text>
                </View>
                {isPro &&
                  (hasActiveSubscription && subscription ? (
                    <SubscriptionStatusBadge status={subscription.status} />
                  ) : (
                    <Badge label="현재 플랜" tone="primary" />
                  ))}
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
                            <Text style={styles.bigPrice}>
                              {pricing.promoPriceKrw.toLocaleString("ko-KR")}원
                            </Text>
                            <Text style={typography.muted}>/ 월</Text>
                          </>
                        ) : (
                          <>
                            <Text style={styles.bigPrice}>
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
                      style={{ alignSelf: "stretch" }}
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
          </>
        )}

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
