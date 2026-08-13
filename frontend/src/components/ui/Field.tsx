import { useState } from "react";
import { Pressable, Text, TextInput, View, type StyleProp, type TextInputProps, type ViewStyle } from "react-native";
import { useTheme, useThemedStyles } from "./ThemeContext";
import { radius } from "./theme";
import { EyeIcon, EyeOffIcon } from "./icons";

interface FieldProps extends TextInputProps {
  label?: string;
  error?: string | null;
  /** 바깥 wrap View에 적용 — flexGrow/minWidth 등 레이아웃 배치용. 입력창 자체 스타일은 `style`을 쓴다. */
  containerStyle?: StyleProp<ViewStyle>;
}

/** `secureTextEntry`를 넘기면 표시/숨기기 토글 버튼이 자동으로 붙는다(비밀번호 입력 전용, Phase 20). */
export function Field({ label, error, style, containerStyle, secureTextEntry, ...rest }: FieldProps) {
  const { colors, typography } = useTheme();
  const [visible, setVisible] = useState(false);
  const styles = useThemedStyles((colors) => ({
    wrap: { gap: 6 },
    inputRow: { justifyContent: "center" as const },
    input: {
      borderWidth: 1,
      borderColor: colors.border,
      borderRadius: radius.md,
      paddingVertical: 10,
      paddingHorizontal: 12,
      fontSize: 15,
      color: colors.text,
      backgroundColor: colors.surface,
    },
    inputError: {
      borderColor: colors.danger,
    },
    toggle: {
      position: "absolute" as const,
      right: 12,
      height: "100%" as const,
      justifyContent: "center" as const,
    },
    error: {
      fontSize: 12,
      color: colors.danger,
    },
  }));
  return (
    <View style={[styles.wrap, containerStyle]}>
      {label ? <Text style={typography.label}>{label}</Text> : null}
      <View style={styles.inputRow}>
        <TextInput
          style={[
            styles.input,
            error ? styles.inputError : null,
            secureTextEntry ? { paddingRight: 44 } : null,
            style,
          ]}
          placeholderTextColor={colors.textFaint}
          secureTextEntry={secureTextEntry && !visible}
          {...rest}
        />
        {secureTextEntry ? (
          <Pressable
            style={styles.toggle}
            onPress={() => setVisible((v) => !v)}
            hitSlop={8}
            accessibilityRole="button"
            accessibilityLabel={visible ? "비밀번호 숨기기" : "비밀번호 표시"}
          >
            {visible ? (
              <EyeOffIcon size={18} color={colors.textFaint} />
            ) : (
              <EyeIcon size={18} color={colors.textFaint} />
            )}
          </Pressable>
        ) : null}
      </View>
      {error ? <Text style={styles.error}>{error}</Text> : null}
    </View>
  );
}
