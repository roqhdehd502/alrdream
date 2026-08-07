import { useState } from "react";
import { Image, Text, View } from "react-native";
import { Link } from "expo-router";
import { useAuth } from "../auth/AuthContext";
import { useGoogleAuthRequest, extractIdToken } from "../auth/google";
import { ApiError } from "../api/client";
import { Button } from "../components/ui/Button";
import { Field } from "../components/ui/Field";
import { ErrorBanner } from "../components/ui/Feedback";
import { useTheme, useThemedStyles } from "../components/ui/ThemeContext";
import { fontFamily } from "../components/ui/theme";

export default function SignInScreen() {
  const { login, loginWithGoogle } = useAuth();
  const { typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    root: { flex: 1, backgroundColor: colors.bg, alignItems: "center" as const, justifyContent: "center" as const, padding: 20 },
    card: { width: "100%" as const, maxWidth: 380, gap: 8, alignItems: "flex-start" as const },
    logo: { width: 44, height: 44, marginBottom: 4, borderRadius: 10 },
    form: { width: "100%" as const, gap: 14, marginTop: 20 },
    link: { marginTop: 20, color: colors.primary, fontSize: 13.5, fontFamily: fontFamily.semibold },
  }));
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [googleSubmitting, setGoogleSubmitting] = useState(false);

  const [request, , promptAsync] = useGoogleAuthRequest();

  const handleSubmit = async () => {
    setError(null);
    setSubmitting(true);
    try {
      await login(email, password);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "로그인에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleGoogle = async () => {
    setError(null);
    setGoogleSubmitting(true);
    try {
      const result = await promptAsync();
      const idToken = extractIdToken(result);
      if (!idToken) {
        if (result?.type !== "cancel" && result?.type !== "dismiss") {
          setError("Google 로그인에 실패했습니다.");
        }
        return;
      }
      await loginWithGoogle(idToken);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Google 로그인에 실패했습니다.");
    } finally {
      setGoogleSubmitting(false);
    }
  };

  return (
    <View style={styles.root}>
      <View style={styles.card}>
        <Image source={require("../../assets/images/icon.png")} style={styles.logo} />
        <Text style={typography.title}>알려드림</Text>
        <Text style={typography.muted}>사업 아이디어를 기획/분석/설계까지, AI와 함께.</Text>

        <View style={styles.form}>
          <Field label="이메일" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" />
          <Field label="비밀번호" value={password} onChangeText={setPassword} secureTextEntry />
          <ErrorBanner message={error} />
          <Button label="로그인" onPress={handleSubmit} loading={submitting} />
          <Button
            label="Google로 계속하기"
            variant="secondary"
            onPress={handleGoogle}
            loading={googleSubmitting}
            disabled={!request}
          />
        </View>

        <Link href="/sign-up" style={styles.link}>
          계정이 없으신가요? 회원가입
        </Link>
      </View>
    </View>
  );
}
