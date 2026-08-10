import { Text, TextInput, View, type TextInputProps } from "react-native";
import { useTheme, useThemedStyles } from "./ThemeContext";
import { radius } from "./theme";

interface FieldProps extends TextInputProps {
  label?: string;
  error?: string | null;
}

export function Field({ label, error, style, ...rest }: FieldProps) {
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
    <View style={styles.wrap}>
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
