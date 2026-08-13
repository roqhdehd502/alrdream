import { useState } from "react";
import { Text, View } from "react-native";
import { Link, useRouter } from "expo-router";
import { authApi } from "../api/auth";
import { ApiError } from "../api/client";
import { Button } from "../components/ui/Button";
import { Field } from "../components/ui/Field";
import { ErrorBanner } from "../components/ui/Feedback";
import { useTheme, useThemedStyles } from "../components/ui/ThemeContext";
import { ThemeMenuButton, themeMenuButtonStyles } from "../components/ui/ThemeMenuButton";
import { fontFamily } from "../components/ui/theme";

export default function ForgotPasswordScreen() {
  const router = useRouter();
  const { typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    root: { flex: 1, backgroundColor: colors.bg, alignItems: "center" as const, justifyContent: "center" as const, padding: 20 },
    card: { width: "100%" as const, maxWidth: 380, gap: 8, alignItems: "flex-start" as const },
    form: { width: "100%" as const, gap: 14, marginTop: 20 },
    link: { marginTop: 20, color: colors.primary, fontSize: 13.5, fontFamily: fontFamily.semibold },
  }));
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async () => {
    setError(null);
    setSubmitting(true);
    try {
      await authApi.requestPasswordReset(email);
      // 코드 재요청을 반복하면 push는 스택에 /reset-password 인스턴스가 계속 쌓인다(Phase 21 전수 점검).
      router.replace({ pathname: "/reset-password", params: { email } });
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "요청에 실패했습니다.");
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
        <Text style={typography.title}>비밀번호 재설정</Text>
        <Text style={typography.muted}>가입한 이메일로 6자리 재설정 코드를 보내드려요.</Text>

        <View style={styles.form}>
          <Field label="이메일" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" />
          <ErrorBanner message={error} />
          <Button label="재설정 코드 받기" onPress={handleSubmit} loading={submitting} disabled={!email.trim()} />
        </View>

        <Link href="/sign-in" style={styles.link}>
          로그인으로 돌아가기
        </Link>
      </View>
    </View>
  );
}
