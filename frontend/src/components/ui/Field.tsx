import { Text, TextInput, View, type StyleProp, type TextInputProps, type ViewStyle } from "react-native";
import { useTheme, useThemedStyles } from "./ThemeContext";
import { radius } from "./theme";

interface FieldProps extends TextInputProps {
  label?: string;
  error?: string | null;
  /** 바깥 wrap View에 적용 — flexGrow/minWidth 등 레이아웃 배치용. 입력창 자체 스타일은 `style`을 쓴다. */
  containerStyle?: StyleProp<ViewStyle>;
}

export function Field({ label, error, style, containerStyle, ...rest }: FieldProps) {
  const { colors, typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    wrap: { gap: 6 },
    input: {
      borderWidth: 1,
      borderColor: colors.border,
      borderRadius: radius.sm,
      paddingVertical: 10,
      paddingHorizontal: 12,
      fontSize: 15,
      color: colors.text,
      backgroundColor: colors.surface,
    },
    inputError: {
      borderColor: colors.danger,
    },
    error: {
      fontSize: 12,
      color: colors.danger,
    },
  }));
  return (
    <View style={[styles.wrap, containerStyle]}>
      {label ? <Text style={typography.label}>{label}</Text> : null}
      <TextInput
        style={[styles.input, error ? styles.inputError : null, style]}
        placeholderTextColor={colors.textFaint}
        {...rest}
      />
      {error ? <Text style={styles.error}>{error}</Text> : null}
    </View>
  );
}
