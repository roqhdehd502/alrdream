import { useCallback, useState } from "react";
import { Pressable, Text, View } from "react-native";
import { useFocusEffect, useRouter } from "expo-router";
import { useAuth } from "../../../auth/AuthContext";
import { workspacesApi } from "../../../api/workspaces";
import { ScreenContainer } from "../../../components/ui/ScreenContainer";
import { Card } from "../../../components/ui/Card";
import { SubscriptionIcon, WorkspaceIcon } from "../../../components/ui/icons";
import { useTheme, useThemedStyles } from "../../../components/ui/ThemeContext";

export default function HomeScreen() {
  const router = useRouter();
  const { member } = useAuth();
  const { colors, typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    wrap: { gap: 20 },
    hubCard: { gap: 4 },
    hubCardRow: { flexDirection: "row" as const, alignItems: "center" as const, gap: 14 },
    hubIcon: {
      width: 44,
      height: 44,
      borderRadius: 14,
      backgroundColor: colors.primarySoft,
      alignItems: "center" as const,
      justifyContent: "center" as const,
    },
    hubTextWrap: { flex: 1, gap: 2 },
  }));

  const [workspaceCount, setWorkspaceCount] = useState<number | null>(null);

  useFocusEffect(
    useCallback(() => {
      workspacesApi
        .list(undefined, 0)
        .then((res) => setWorkspaceCount(res.page.totalElements))
        .catch(() => setWorkspaceCount(null));
    }, []),
  );

  // member.plan은 결제 구독뿐 아니라 쿠폰/관리자 지급으로도 PRO가 되므로 이 값 하나만 보면 된다
  // (subscription.status는 "관리할 결제 구독이 있는지"만 알려줄 뿐 Pro 여부의 전체 신호가 아니다).
  const isPro = member?.plan === "PRO";

  return (
    <ScreenContainer>
      <View style={styles.wrap}>
        <Text style={typography.title}>안녕하세요{member?.email ? `, ${member.email.split("@")[0]}님` : ""}</Text>

        <Pressable onPress={() => router.push("/workspaces")}>
          <Card style={styles.hubCard}>
            <View style={styles.hubCardRow}>
              <View style={styles.hubIcon}>
                <WorkspaceIcon size={20} color={colors.primary} />
              </View>
              <View style={styles.hubTextWrap}>
                <Text style={typography.heading}>워크스페이스</Text>
                <Text style={typography.muted}>
                  {workspaceCount === null ? "불러오는 중..." : `${workspaceCount}개의 워크스페이스`}
                </Text>
              </View>
            </View>
          </Card>
        </Pressable>

        <Pressable onPress={() => router.push("/subscription")}>
          <Card style={styles.hubCard}>
            <View style={styles.hubCardRow}>
              <View style={styles.hubIcon}>
                <SubscriptionIcon size={20} color={colors.primary} />
              </View>
              <View style={styles.hubTextWrap}>
                <Text style={typography.heading}>구독</Text>
                <Text style={typography.muted}>
                  {member === null ? "불러오는 중..." : isPro ? "Pro 플랜 이용 중" : "Free 플랜 — Pro로 업그레이드해보세요"}
                </Text>
              </View>
            </View>
          </Card>
        </Pressable>
      </View>
    </ScreenContainer>
  );
}
