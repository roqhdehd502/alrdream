import { useState } from "react";
import { Text, View } from "react-native";
import { useAuth } from "../../auth/AuthContext";
import { couponApi } from "../../api/coupon";
import { ApiError } from "../../api/client";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { Field } from "../../components/ui/Field";
import { ErrorBanner } from "../../components/ui/Feedback";
import { ScreenContainer } from "../../components/ui/ScreenContainer";
import { useTheme, useThemedStyles } from "../../components/ui/ThemeContext";

function formatDate(value: string) {
  return new Date(value).toLocaleDateString("ko-KR");
}

export default function CouponScreen() {
  const { refreshMember } = useAuth();
  const { typography } = useTheme();
  const styles = useThemedStyles(() => ({
    wrap: { gap: 16 },
    field: { alignSelf: "stretch" as const },
  }));

  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<{ benefitDays: number; proExpiresAt: string } | null>(null);

  const redeem = async () => {
    if (!code.trim()) {
      setError("쿠폰 코드를 입력해주세요.");
      return;
    }
    setBusy(true);
    setError(null);
    setResult(null);
    try {
      const res = await couponApi.redeem(code.trim());
      setResult(res);
      setCode("");
      refreshMember();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "쿠폰 등록에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <ScreenContainer>
      <View style={styles.wrap}>
        <Text style={typography.muted}>이벤트로 공개된 쿠폰 코드를 입력하면 Pro를 이용할 수 있습니다.</Text>

        <Field
          placeholder="쿠폰 코드"
          value={code}
          onChangeText={setCode}
          autoCapitalize="characters"
          containerStyle={styles.field}
        />

        <ErrorBanner message={error} />

        {result && (
          <Card tone="default">
            <Text style={typography.heading}>등록되었습니다</Text>
            <Text style={typography.muted}>
              Pro {result.benefitDays}일이 지급/연장되었습니다. (보장 만료: {formatDate(result.proExpiresAt)})
            </Text>
          </Card>
        )}

        <Button label="쿠폰 등록" onPress={redeem} loading={busy} style={{ alignSelf: "flex-start" }} />
      </View>
    </ScreenContainer>
  );
}
