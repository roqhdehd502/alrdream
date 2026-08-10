import { useState } from "react";
import { Modal, Pressable, StyleProp, StyleSheet, Text, View, ViewStyle } from "react-native";
import { useTheme, useThemedStyles, type ThemePreference } from "./ThemeContext";
import { fontFamily, radius } from "./theme";
import { MoonIcon, SunIcon } from "./icons";

const OPTIONS: { key: ThemePreference; label: string }[] = [
  { key: "system", label: "시스템 설정" },
  { key: "light", label: "라이트" },
  { key: "dark", label: "다크" },
];

export function ThemeMenuButton({ style }: { style?: StyleProp<ViewStyle> }) {
  const { colors, scheme, preference, setPreference } = useTheme();
  const [open, setOpen] = useState(false);
  const styles = useThemedStyles((colors) => ({
    trigger: {
      width: 34,
      height: 34,
      borderRadius: 17,
      alignItems: "center" as const,
      justifyContent: "center" as const,
      backgroundColor: colors.surface,
      borderWidth: 1,
      borderColor: colors.border,
    },
    backdrop: { flex: 1 },
    menu: {
      position: "absolute" as const,
      top: 60,
      right: 20,
      backgroundColor: colors.surface,
      borderWidth: 1,
      borderColor: colors.border,
      borderRadius: radius.sm,
      paddingVertical: 6,
      minWidth: 148,
      shadowColor: "#000",
      shadowOpacity: 0.18,
      shadowRadius: 14,
      shadowOffset: { width: 0, height: 6 },
      elevation: 6,
    },
    option: { paddingVertical: 10, paddingHorizontal: 14 },
    optionLabel: { fontSize: 13.5, fontFamily: fontFamily.medium, color: colors.textMuted },
    optionLabelActive: { color: colors.primaryHover, fontFamily: fontFamily.semibold },
  }));

  return (
    <>
      <Pressable
        style={[styles.trigger, style]}
        onPress={() => setOpen(true)}
        hitSlop={8}
        accessibilityLabel="테마 전환"
        accessibilityRole="button"
      >
        {scheme === "dark" ? <SunIcon size={16} color={colors.text} /> : <MoonIcon size={16} color={colors.text} />}
      </Pressable>
      <Modal visible={open} transparent animationType="fade" onRequestClose={() => setOpen(false)}>
        <Pressable style={styles.backdrop} onPress={() => setOpen(false)}>
          <View style={styles.menu}>
            {OPTIONS.map((opt) => {
              const active = preference === opt.key;
              return (
                <Pressable
                  key={opt.key}
                  style={styles.option}
                  onPress={() => {
                    setPreference(opt.key);
                    setOpen(false);
                  }}
                >
                  <Text style={[styles.optionLabel, active && styles.optionLabelActive]}>
                    {active ? "✓ " : ""}
                    {opt.label}
                  </Text>
                </Pressable>
              );
            })}
          </View>
        </Pressable>
      </Modal>
    </>
  );
}

export const themeMenuButtonStyles = StyleSheet.create({
  floating: {
    position: "absolute",
    top: 16,
    right: 16,
  },
});
