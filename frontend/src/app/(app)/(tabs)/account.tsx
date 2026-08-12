import { useEffect, useState } from "react";
import { Text, View } from "react-native";
import { useRouter } from "expo-router";
import { useAuth } from "../../../auth/AuthContext";
import { authApi } from "../../../api/auth";
import { usageQuotaApi } from "../../../api/usageQuota";
import { ApiError } from "../../../api/client";
import { Avatar } from "../../../components/ui/Avatar";
import { Button } from "../../../components/ui/Button";
import { Card } from "../../../components/ui/Card";
import { ErrorBanner, Loading } from "../../../components/ui/Feedback";
import { Field } from "../../../components/ui/Field";
import { IconChip } from "../../../components/ui/IconChip";
import { ProgressRing } from "../../../components/ui/ProgressRing";
import { ScreenContainer } from "../../../components/ui/ScreenContainer";
import { useTheme, useThemedStyles } from "../../../components/ui/ThemeContext";
import { SubscriptionIcon, TicketIcon } from "../../../components/ui/icons";
import type { UsageQuotaResponse } from "../../../types";

export default function AccountScreen() {
  const router = useRouter();
  const { member, logout, withdraw, refreshMember } = useAuth();
  const { colors, typography } = useTheme();
  const styles = useThemedStyles(() => ({
    wrap: { gap: 28 },
    section: { gap: 10 },
    dangerSection: { gap: 10 },
    dangerHeading: { color: colors.danger },
    confirmButtons: { flexDirection: "row" as const, gap: 10 },
    saveButton: { alignSelf: "flex-start" as const },
    profileRow: {
      flexDirection: "row" as const,
      alignItems: "center" as const,
      gap: 14,
    },
    profileTextWrap: { gap: 2 },
    nameRow: {
      flexDirection: "row" as const,
      gap: 10,
      alignItems: "flex-end" as const,
    },
    nameField: { flexGrow: 1 },
    quotaRow: {
      flexDirection: "row" as const,
      alignItems: "center" as const,
      gap: 16,
    },
    navHeadingRow: {
      flexDirection: "row" as const,
      alignItems: "center" as const,
      gap: 10,
    },
  }));
  // 회원 탈퇴는 되돌릴 수 없어 2단계로 나눈다: 1) 계정 정보로 본인 확인, 2) 확인 문구를 정확히 입력.
  const [withdrawStep, setWithdrawStep] = useState<
    "idle" | "verify" | "confirm"
  >("idle");
  const [verifyInput, setVerifyInput] = useState("");
  const [verifying, setVerifying] = useState(false);
  const [verifyError, setVerifyError] = useState<string | null>(null);
  const [confirmPhraseInput, setConfirmPhraseInput] = useState("");
  const [withdrawing, setWithdrawing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [quota, setQuota] = useState<UsageQuotaResponse | null>(null);
  const WITHDRAW_PHRASE = "탈퇴합니다";

  const [nameInput, setNameInput] = useState(member?.name ?? "");
  const [syncedName, setSyncedName] = useState(member?.name ?? null);
  const [savingName, setSavingName] = useState(false);
  const [nameMessage, setNameMessage] = useState<string | null>(null);

  // member.name이 (새로고침 등으로) 바뀌면 편집 중이 아닌 입력값을 따라가게 한다 — useEffect 대신 렌더 중
  // 조정하는 방식(React 권장 패턴, "Adjusting state when a prop changes")으로 불필요한 추가 렌더를 피한다.
  if ((member?.name ?? null) !== syncedName) {
    setSyncedName(member?.name ?? null);
    setNameInput(member?.name ?? "");
  }

  useEffect(() => {
    usageQuotaApi
      .getCurrent()
      .then(setQuota)
      .catch(() => setQuota(null));
  }, []);

  const cancelWithdrawFlow = () => {
    setWithdrawStep("idle");
    setVerifyInput("");
    setVerifyError(null);
    setConfirmPhraseInput("");
  };

  // LOCAL 계정은 현재 비밀번호로 실제 인증(로그인 API 재사용, 세션 토큰은 저장하지 않고 성공 여부만 본다),
  // 소셜 로그인 계정은 비밀번호가 없어 이메일 재입력으로 대체한다.
  const handleVerify = async () => {
    if (!member) return;
    setVerifyError(null);
    if (member.provider === "LOCAL") {
      setVerifying(true);
      try {
        await authApi.login(member.email, verifyInput);
        setWithdrawStep("confirm");
      } catch (e) {
        setVerifyError(
          e instanceof ApiError ? e.message : "인증에 실패했습니다.",
        );
      } finally {
        setVerifying(false);
      }
      return;
    }
    if (verifyInput.trim().toLowerCase() !== member.email.toLowerCase()) {
      setVerifyError("이메일이 일치하지 않습니다.");
      return;
    }
    setWithdrawStep("confirm");
  };

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

  const handleSaveName = async () => {
    setSavingName(true);
    setNameMessage(null);
    try {
      await authApi.updateName(nameInput.trim());
      await refreshMember();
      setNameMessage("저장되었습니다.");
    } catch (e) {
      setNameMessage(
        e instanceof ApiError ? e.message : "저장에 실패했습니다.",
      );
    } finally {
      setSavingName(false);
    }
  };

  const quotaRatio =
    quota && quota.limitCount > 0
      ? Math.min(quota.generationCount / quota.limitCount, 1)
      : 0;
  const displayName = member
    ? (member.name ?? member.email.split("@")[0])
    : null;

  return (
    <ScreenContainer>
      <View style={styles.wrap}>
        <View style={styles.section}>
          <Text style={typography.heading}>계정 정보</Text>
          <Card style={styles.section}>
            <View style={styles.profileRow}>
              {member && (
                <Avatar
                  seed={member.id}
                  label={displayName ?? member.email}
                  size={56}
                />
              )}
              <View style={styles.profileTextWrap}>
                <Text style={typography.body}>{member?.email}</Text>
                <Text style={typography.muted}>
                  플랜: {member?.plan === "PRO" ? "Pro" : "Free"}
                </Text>
              </View>
            </View>

            <View style={styles.nameRow}>
              <Field
                placeholder="기본값은 이메일로 표시됩니다"
                value={nameInput}
                onChangeText={setNameInput}
                containerStyle={styles.nameField}
              />
              <Button
                label="저장"
                variant="secondary"
                onPress={handleSaveName}
                loading={savingName}
              />
            </View>
            {nameMessage && <Text style={typography.muted}>{nameMessage}</Text>}

            <Button
              label="로그아웃"
              variant="secondary"
              onPress={logout}
              style={styles.saveButton}
            />
          </Card>
        </View>

        <View style={styles.section}>
          <Text style={typography.heading}>이번 달 AI 생성 사용량</Text>
          {quota === null ? (
            <Loading />
          ) : (
            <Card style={styles.quotaRow}>
              <ProgressRing
                progress={quotaRatio}
                size={64}
                strokeWidth={7}
                label={`${Math.round(quotaRatio * 100)}%`}
              />
              <View style={{ gap: 2 }}>
                <Text style={typography.body}>
                  {quota.generationCount} / {quota.limitCount}회 사용
                </Text>
                <Text style={typography.muted}>{quota.period}</Text>
              </View>
            </Card>
          )}
        </View>

        <View style={styles.section}>
          <View style={styles.navHeadingRow}>
            <IconChip
              icon={<SubscriptionIcon size={16} color={colors.primary} />}
              size={32}
              tone="surface"
            />
            <Text style={typography.heading}>구독 관리</Text>
          </View>
          <Text style={typography.muted}>
            구독 상태, 결제 내역을 확인할 수 있습니다.
          </Text>
          <Button
            label="구독 보기"
            variant="secondary"
            onPress={() => router.push("/subscription")}
            style={styles.saveButton}
          />
        </View>

        <View style={styles.section}>
          <View style={styles.navHeadingRow}>
            <IconChip
              icon={<TicketIcon size={16} color={colors.primary} />}
              size={32}
              tone="surface"
            />
            <Text style={typography.heading}>쿠폰</Text>
          </View>
          <Text style={typography.muted}>
            쿠폰 코드를 등록하면 Pro를 무료로 이용할 수 있습니다.
          </Text>
          <Button
            label="쿠폰 등록"
            variant="secondary"
            onPress={() => router.push("/coupon")}
            style={styles.saveButton}
          />
        </View>

        <Card tone="danger" style={styles.dangerSection}>
          <Text style={[typography.heading, styles.dangerHeading]}>
            회원 탈퇴
          </Text>
          <Text style={typography.muted}>
            탈퇴하면 다시 로그인할 수 없습니다.
          </Text>
          <Text style={typography.muted}>
            워크스페이스 등 데이터는 즉시 조회할 수 없게 되며, 같은 이메일로
            재가입은 가능합니다.
          </Text>
          <Text style={typography.muted}>
            구독 중이라면 먼저 구독을 해지해야 합니다.
          </Text>
          <ErrorBanner message={error} />

          {withdrawStep === "idle" && (
            <Button
              label="회원 탈퇴"
              variant="danger"
              onPress={() => setWithdrawStep("verify")}
              style={styles.saveButton}
            />
          )}

          {withdrawStep === "verify" && (
            <View style={styles.section}>
              {member?.provider === "LOCAL" ? (
                <Field
                  label="현재 비밀번호로 본인 확인"
                  value={verifyInput}
                  onChangeText={setVerifyInput}
                  secureTextEntry
                />
              ) : (
                <Field
                  label={`본인 확인을 위해 이메일(${member?.email})을 입력하세요`}
                  value={verifyInput}
                  onChangeText={setVerifyInput}
                  autoCapitalize="none"
                  keyboardType="email-address"
                />
              )}
              <ErrorBanner message={verifyError} />
              <View style={styles.confirmButtons}>
                <Button
                  label="취소"
                  variant="secondary"
                  onPress={cancelWithdrawFlow}
                />
                <Button
                  label="확인"
                  variant="danger"
                  onPress={handleVerify}
                  loading={verifying}
                />
              </View>
            </View>
          )}

          {withdrawStep === "confirm" && (
            <View style={styles.section}>
              <Text style={typography.muted}>
                계속하려면 아래 입력란에 “{WITHDRAW_PHRASE}”를 정확히
                입력하세요.
              </Text>
              <Field
                value={confirmPhraseInput}
                onChangeText={setConfirmPhraseInput}
                placeholder={WITHDRAW_PHRASE}
                autoCapitalize="none"
              />
              <View style={styles.confirmButtons}>
                <Button
                  label="취소"
                  variant="secondary"
                  onPress={cancelWithdrawFlow}
                />
                <Button
                  label="정말 탈퇴"
                  variant="danger"
                  onPress={handleWithdraw}
                  loading={withdrawing}
                  disabled={confirmPhraseInput !== WITHDRAW_PHRASE}
                />
              </View>
            </View>
          )}
        </Card>
      </View>
    </ScreenContainer>
  );
}
