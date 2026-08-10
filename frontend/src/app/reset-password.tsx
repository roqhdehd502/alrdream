import { useState } from "react";
import { Text, View } from "react-native";
import { Link, useLocalSearchParams, useRouter } from "expo-router";
import { authApi } from "../api/auth";
import { ApiError } from "../api/client";
import { Button } from "../components/ui/Button";
import { Field } from "../components/ui/Field";
import { ErrorBanner } from "../components/ui/Feedback";
import { useTheme, useThemedStyles } from "../components/ui/ThemeContext";
import { ThemeMenuButton, themeMenuButtonStyles } from "../components/ui/ThemeMenuButton";
import { fontFamily } from "../components/ui/theme";

export default function ResetPasswordScreen() {
  const router = useRouter();
  const { email: emailParam } = useLocalSearchParams<{ email?: string }>();
  const { typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    root: { flex: 1, backgroundColor: colors.bg, alignItems: "center" as const, justifyContent: "center" as const, padding: 20 },
    card: { width: "100%" as const, maxWidth: 380, gap: 8, alignItems: "flex-start" as const },
    form: { width: "100%" as const, gap: 14, marginTop: 20 },
    link: { marginTop: 20, color: colors.primary, fontSize: 13.5, fontFamily: fontFamily.semibold },
  }));
  const [email, setEmail] = useState(emailParam ?? "");
  const [code, setCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async () => {
    setError(null);
    if (newPassword.length < 8) {
      setError("비밀번호는 8자 이상이어야 합니다.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setError("비밀번호가 일치하지 않습니다.");
      return;
    }
    setSubmitting(true);
    try {
      await authApi.confirmPasswordReset(email, code, newPassword);
      router.replace("/sign-in");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "재설정에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <View style={styles.root}>
      <View style={themeMenuButtonStyles.floating}>
        <ThemeMenuButton />
      </View>
      <View style={styles.card}>
        <Text style={typography.title}>코드 확인</Text>
        <Text style={typography.muted}>이메일로 받은 6자리 코드와 새 비밀번호를 입력해주세요.</Text>

        <View style={styles.form}>
          <Field label="이메일" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" />
          <Field label="6자리 코드" value={code} onChangeText={setCode} keyboardType="number-pad" maxLength={6} />
          <Field label="새 비밀번호 (8자 이상)" value={newPassword} onChangeText={setNewPassword} secureTextEntry />
          <Field label="새 비밀번호 확인" value={confirmPassword} onChangeText={setConfirmPassword} secureTextEntry />
          <ErrorBanner message={error} />
          <Button
            label="비밀번호 재설정"
            onPress={handleSubmit}
            loading={submitting}
            disabled={!email.trim() || code.length !== 6 || !newPassword}
          />
        </View>

        <Link href="/sign-in" style={styles.link}>
          로그인으로 돌아가기
        </Link>
      </View>
    </View>
  );
}
