import { useState } from "react";
import { Image, StyleSheet, Text, View } from "react-native";
import { Link } from "expo-router";
import { useAuth } from "../auth/AuthContext";
import { useGoogleAuthRequest, extractIdToken } from "../auth/google";
import { ApiError } from "../api/client";
import { Button } from "../components/ui/Button";
import { Field } from "../components/ui/Field";
import { ErrorBanner } from "../components/ui/Feedback";
import { colors, typography } from "../components/ui/theme";

export default function SignUpScreen() {
  const { signup, loginWithGoogle } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [googleSubmitting, setGoogleSubmitting] = useState(false);

  const [request, , promptAsync] = useGoogleAuthRequest();

  const handleSubmit = async () => {
    setError(null);
    if (password.length < 8) {
      setError("비밀번호는 8자 이상이어야 합니다.");
      return;
    }
    if (password !== confirmPassword) {
      setError("비밀번호가 일치하지 않습니다.");
      return;
    }
    setSubmitting(true);
    try {
      await signup(email, password);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "회원가입에 실패했습니다.");
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
          setError("Google 회원가입에 실패했습니다.");
        }
        return;
      }
      await loginWithGoogle(idToken);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Google 회원가입에 실패했습니다.");
    } finally {
      setGoogleSubmitting(false);
    }
  };

  return (
    <View style={styles.root}>
      <View style={styles.card}>
        <Image source={require("../../assets/images/icon.png")} style={styles.logo} />
        <Text style={typography.title}>회원가입</Text>
        <Text style={typography.muted}>몇 초면 시작할 수 있어요.</Text>

        <View style={styles.form}>
          <Field label="이메일" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" />
          <Field label="비밀번호 (8자 이상)" value={password} onChangeText={setPassword} secureTextEntry />
          <Field label="비밀번호 확인" value={confirmPassword} onChangeText={setConfirmPassword} secureTextEntry />
          <ErrorBanner message={error} />
          <Button label="회원가입" onPress={handleSubmit} loading={submitting} />
          <Button
            label="Google로 계속하기"
            variant="secondary"
            onPress={handleGoogle}
            loading={googleSubmitting}
            disabled={!request}
          />
        </View>

        <Link href="/sign-in" style={styles.link}>
          이미 계정이 있으신가요? 로그인
        </Link>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg, alignItems: "center", justifyContent: "center", padding: 20 },
  card: { width: "100%", maxWidth: 380, gap: 8, alignItems: "flex-start" },
  logo: { width: 44, height: 44, marginBottom: 4, borderRadius: 10 },
  form: { width: "100%", gap: 14, marginTop: 20 },
  link: { marginTop: 20, color: colors.primary, fontSize: 13.5, fontWeight: "600" },
});
