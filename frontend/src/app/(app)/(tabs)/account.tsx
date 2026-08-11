import { useEffect, useState } from "react";
import { Text, View } from "react-native";
import { useRouter } from "expo-router";
import { useAuth } from "../../../auth/AuthContext";
import { usageQuotaApi } from "../../../api/usageQuota";
import { ApiError } from "../../../api/client";
import { Button } from "../../../components/ui/Button";
import { Card } from "../../../components/ui/Card";
import { ErrorBanner, Loading } from "../../../components/ui/Feedback";
import { ScreenContainer } from "../../../components/ui/ScreenContainer";
import { useTheme, useThemedStyles } from "../../../components/ui/ThemeContext";
import type { UsageQuotaResponse } from "../../../types";

export default function AccountScreen() {
  const router = useRouter();
  const { member, logout, withdraw } = useAuth();
  const { typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    wrap: { gap: 28 },
    section: { gap: 10 },
    dangerSection: { gap: 10 },
    dangerHeading: { color: colors.danger },
    confirmButtons: { flexDirection: "row" as const, gap: 10 },
    saveButton: { alignSelf: "flex-start" as const },
    row: { flexDirection: "row" as const, justifyContent: "space-between" as const },
    quotaBarTrack: { height: 8, borderRadius: 4, backgroundColor: colors.border, overflow: "hidden" as const },
    quotaBarFill: { height: 8, borderRadius: 4, backgroundColor: colors.primary },
  }));
  const [confirmingWithdraw, setConfirmingWithdraw] = useState(false);
  const [withdrawing, setWithdrawing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [quota, setQuota] = useState<UsageQuotaResponse | null>(null);

  useEffect(() => {
    usageQuotaApi.getCurrent().then(setQuota).catch(() => setQuota(null));
  }, []);

  const handleWithdraw = async () => {
    setWithdrawing(true);
    setError(null);
    try {
      await withdraw();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "탈퇴에 실패했습니다.");
      setWithdrawing(false);
    }
  };

  const quotaRatio = quota && quota.limitCount > 0 ? Math.min(quota.generationCount / quota.limitCount, 1) : 0;

  return (
    <ScreenContainer>
      <View style={styles.wrap}>
        <View style={styles.section}>
          <Text style={typography.heading}>계정 정보</Text>
          <Text style={typography.muted}>{member?.email}</Text>
          <Text style={typography.muted}>플랜: {member?.plan === "PRO" ? "Pro" : "Free"}</Text>
          <Button label="로그아웃" variant="secondary" onPress={logout} style={styles.saveButton} />
        </View>

        <View style={styles.section}>
          <Text style={typography.heading}>이번 달 AI 생성 사용량</Text>
          {quota === null ? (
            <Loading />
          ) : (
            <Card style={styles.section}>
              {quota.plan === "PRO" ? (
                <Text style={typography.body}>무제한 (Pro 플랜)</Text>
              ) : (
                <>
                  <View style={styles.row}>
                    <Text style={typography.body}>
                      {quota.generationCount} / {quota.limitCount}회 사용
                    </Text>
                    <Text style={typography.muted}>{quota.period}</Text>
                  </View>
                  <View style={styles.quotaBarTrack}>
                    <View style={[styles.quotaBarFill, { width: `${quotaRatio * 100}%` }]} />
                  </View>
                </>
              )}
            </Card>
          )}
        </View>

        <View style={styles.section}>
          <Text style={typography.heading}>구독 관리</Text>
          <Text style={typography.muted}>구독 상태, 결제 내역을 확인할 수 있습니다.</Text>
          <Button
            label="구독 보기"
            variant="secondary"
            onPress={() => router.push("/subscription")}
            style={styles.saveButton}
          />
        </View>

        <View style={styles.section}>
          <Text style={typography.heading}>쿠폰</Text>
          <Text style={typography.muted}>쿠폰 코드를 등록하면 Pro를 무료로 이용할 수 있습니다.</Text>
          <Button label="쿠폰 등록" variant="secondary" onPress={() => router.push("/coupon")} style={styles.saveButton} />
        </View>

        <Card tone="danger" style={styles.dangerSection}>
          <Text style={[typography.heading, styles.dangerHeading]}>회원 탈퇴</Text>
          <Text style={typography.muted}>
            탈퇴하면 다시 로그인할 수 없습니다. 워크스페이스 등 데이터는 즉시 조회할 수 없게 되며, 같은
            이메일로 재가입은 가능합니다. 구독 중이라면 먼저 구독을 해지해야 합니다.
          </Text>
          <ErrorBanner message={error} />
          {!confirmingWithdraw ? (
            <Button
              label="회원 탈퇴"
              variant="danger"
              onPress={() => setConfirmingWithdraw(true)}
              style={styles.saveButton}
            />
          ) : (
            <View style={styles.confirmButtons}>
              <Button label="취소" variant="secondary" onPress={() => setConfirmingWithdraw(false)} />
              <Button label="정말 탈퇴" variant="danger" onPress={handleWithdraw} loading={withdrawing} />
            </View>
          )}
        </Card>
      </View>
    </ScreenContainer>
  );
}
