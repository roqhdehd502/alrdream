import { useState } from "react";
import { Text, View } from "react-native";
import { useAuth } from "../../auth/AuthContext";
import { ApiError } from "../../api/client";
import { Button } from "../../components/ui/Button";
import { ErrorBanner } from "../../components/ui/Feedback";
import { ScreenContainer } from "../../components/ui/ScreenContainer";
import { useTheme, useThemedStyles } from "../../components/ui/ThemeContext";

export default function AccountScreen() {
  const { member, logout, withdraw } = useAuth();
  const { typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    wrap: { gap: 28 },
    section: { gap: 10 },
    dangerSection: {
      gap: 10,
      borderWidth: 1,
      borderColor: colors.dangerSoft,
      backgroundColor: colors.dangerSoft,
      padding: 16,
      borderRadius: 14,
    },
    dangerHeading: { color: colors.danger },
    confirmButtons: { flexDirection: "row" as const, gap: 10 },
    saveButton: { alignSelf: "flex-start" as const },
  }));
  const [confirmingWithdraw, setConfirmingWithdraw] = useState(false);
  const [withdrawing, setWithdrawing] = useState(false);
  const [error, setError] = useState<string | null>(null);

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

  return (
    <ScreenContainer>
      <View style={styles.wrap}>
        <View style={styles.section}>
          <Text style={typography.heading}>계정 정보</Text>
          <Text style={typography.muted}>{member?.email}</Text>
          <Button label="로그아웃" variant="secondary" onPress={logout} style={styles.saveButton} />
        </View>

        <View style={styles.dangerSection}>
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
        </View>
      </View>
    </ScreenContainer>
  );
}
