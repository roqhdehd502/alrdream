import { useEffect, useState } from "react";
import { Image, Platform, Text, View } from "react-native";
import { Link } from "expo-router";
import { useAuth } from "../auth/AuthContext";
import { useGoogleAuthRequest, extractIdToken } from "../auth/google";
import { authApi } from "../api/auth";
import { ApiError } from "../api/client";
import { Button } from "../components/ui/Button";
import { Field } from "../components/ui/Field";
import { ErrorBanner } from "../components/ui/Feedback";
import { GoogleIcon } from "../components/ui/icons";
import { useTheme, useThemedStyles } from "../components/ui/ThemeContext";
import { ThemeMenuButton, themeMenuButtonStyles } from "../components/ui/ThemeMenuButton";
import { fontFamily } from "../components/ui/theme";

type Step = "email" | "code" | "password";

const STEP_TITLE: Record<Step, string> = {
  email: "회원가입",
  code: "이메일 인증",
  password: "비밀번호 설정",
};

const STEP_SUBTITLE: Record<Step, string> = {
  email: "이메일을 인증하고 시작할게요.",
  code: "받은 코드를 입력해주세요.",
  password: "마지막으로 비밀번호만 정하면 끝이에요.",
};

function formatRemaining(totalSeconds: number) {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, "0")}`;
}

export default function SignUpScreen() {
  const { signup, loginWithGoogle } = useAuth();
  const { typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    root: { flex: 1, backgroundColor: colors.bg, alignItems: "center" as const, justifyContent: "center" as const, padding: 20 },
    card: { width: "100%" as const, maxWidth: 380, gap: 8, alignItems: "flex-start" as const },
    logo: { width: 44, height: 44, marginBottom: 4, borderRadius: 10 },
    form: { width: "100%" as const, gap: 14, marginTop: 20 },
    link: { marginTop: 20, color: colors.primary, fontSize: 13.5, fontFamily: fontFamily.semibold },
    backLink: { color: colors.textMuted, fontSize: 13.5, fontFamily: fontFamily.semibold },
    expiry: { color: colors.textMuted, fontSize: 13, fontFamily: fontFamily.medium },
    expiryUrgent: { color: colors.danger, fontFamily: fontFamily.semibold },
  }));

  const [step, setStep] = useState<Step>("email");
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [requestingCode, setRequestingCode] = useState(false);
  const [confirmingCode, setConfirmingCode] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [googleSubmitting, setGoogleSubmitting] = useState(false);
  const [codeExpiresAt, setCodeExpiresAt] = useState<number | null>(null);
  const [remainingSeconds, setRemainingSeconds] = useState(0);

  const [request, , promptAsync] = useGoogleAuthRequest();

  // 코드 화면에 있는 동안 1초마다 만료까지 남은 시간을 갱신한다.
  useEffect(() => {
    if (step !== "code" || codeExpiresAt === null) return;
    const tick = () => setRemainingSeconds(Math.max(0, Math.round((codeExpiresAt - Date.now()) / 1000)));
    tick();
    const interval = setInterval(tick, 1000);
    return () => clearInterval(interval);
  }, [step, codeExpiresAt]);

  const handleRequestCode = async () => {
    setError(null);
    setRequestingCode(true);
    try {
      const { expiresInSeconds } = await authApi.requestSignupVerification(email.trim());
      setCodeExpiresAt(Date.now() + expiresInSeconds * 1000);
      setStep("code");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "코드 발송에 실패했습니다.");
    } finally {
      setRequestingCode(false);
    }
  };

  const handleConfirmCode = async () => {
    setError(null);
    setConfirmingCode(true);
    try {
      await authApi.confirmSignupVerification(email.trim(), code);
      setStep("password");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "인증에 실패했습니다.");
    } finally {
      setConfirmingCode(false);
    }
  };

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
      await signup(email.trim(), password);
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
      const idToken = extractIdToken(request, result);
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
      <View style={themeMenuButtonStyles.floating}>
        <ThemeMenuButton />
      </View>
      <View style={styles.card}>
        <Image source={require("../../assets/images/icon.png")} style={styles.logo} />
        <Text style={typography.title}>{STEP_TITLE[step]}</Text>
        <Text style={typography.muted}>{STEP_SUBTITLE[step]}</Text>

        {step === "email" && (
          <View style={styles.form}>
            <Field
              label="이메일"
              value={email}
              onChangeText={setEmail}
              autoCapitalize="none"
              keyboardType="email-address"
            />
            <ErrorBanner message={error} />
            <Button
              label="인증 코드 받기"
              onPress={handleRequestCode}
              loading={requestingCode}
              disabled={!email.trim()}
            />
            {/* Google OAuth 클라이언트가 Web application 타입이라 https/localhost 리디렉션만 허용된다 —
                alrdream:// 같은 커스텀 스킴은 등록 자체가 안 돼 네이티브 빌드에서는 항상 실패한다. 웹에서만 노출. */}
            {Platform.OS === "web" && (
              <Button
                label="Google로 계속하기"
                variant="secondary"
                icon={<GoogleIcon size={18} />}
                onPress={handleGoogle}
                loading={googleSubmitting}
                disabled={!request}
              />
            )}
          </View>
        )}

        {step === "code" && (
          <View style={styles.form}>
            <Text style={typography.muted}>{email}로 인증 코드를 보냈습니다.</Text>
            <Field
              label="6자리 코드"
              value={code}
              onChangeText={setCode}
              keyboardType="number-pad"
              maxLength={6}
            />
            <Text style={[styles.expiry, remainingSeconds <= 0 && styles.expiryUrgent]}>
              {remainingSeconds > 0
                ? `코드 만료까지 ${formatRemaining(remainingSeconds)} 남음`
                : "코드가 만료되었습니다. 다시 받아주세요."}
            </Text>
            <ErrorBanner message={error} />
            <Button
              label="확인"
              onPress={handleConfirmCode}
              loading={confirmingCode}
              disabled={code.length !== 6 || remainingSeconds <= 0}
            />
            <Button
              label="코드 다시 받기"
              variant="secondary"
              onPress={handleRequestCode}
              loading={requestingCode}
            />
          </View>
        )}

        {step === "password" && (
          <View style={styles.form}>
            <Text style={typography.muted}>{email} 인증 완료</Text>
            <Field label="비밀번호 (8자 이상)" value={password} onChangeText={setPassword} secureTextEntry />
            <Field label="비밀번호 확인" value={confirmPassword} onChangeText={setConfirmPassword} secureTextEntry />
            <ErrorBanner message={error} />
            <Button label="회원가입" onPress={handleSubmit} loading={submitting} />
          </View>
        )}

        {step !== "email" && (
          <Text
            style={styles.backLink}
            onPress={() => {
              setError(null);
              setStep(step === "password" ? "code" : "email");
            }}
          >
            {step === "password" ? "인증 코드 다시 입력" : "이메일 다시 입력"}
          </Text>
        )}

        <Link href="/sign-in" style={styles.link}>
          이미 계정이 있으신가요? 로그인
        </Link>
      </View>
    </View>
  );
}
