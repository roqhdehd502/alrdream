import { useCallback, useState } from "react";
import { Pressable, Text, View } from "react-native";
import { useFocusEffect, useRouter } from "expo-router";
import { useAuth } from "../../../auth/AuthContext";
import { workspacesApi } from "../../../api/workspaces";
import { ScreenContainer } from "../../../components/ui/ScreenContainer";
import { Card } from "../../../components/ui/Card";
import { Avatar } from "../../../components/ui/Avatar";
import { IconChip } from "../../../components/ui/IconChip";
import { SubscriptionIcon, WorkspaceIcon } from "../../../components/ui/icons";
import { useTheme, useThemedStyles } from "../../../components/ui/ThemeContext";

export default function HomeScreen() {
  const router = useRouter();
  const { member } = useAuth();
  const { colors, typography } = useTheme();
  const styles = useThemedStyles(() => ({
    wrap: { gap: 20 },
    greetingRow: { flexDirection: "row" as const, alignItems: "center" as const, gap: 12 },
    hubCard: { gap: 4 },
    hubCardRow: { flexDirection: "row" as const, alignItems: "center" as const, gap: 14 },
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
  // 이름을 설정하지 않았으면 이메일 앞부분을 기본값으로 보여준다(Phase 20 — 표시 로직은 프론트 책임).
  const displayName = member ? member.name ?? member.email.split("@")[0] : null;

  return (
    <ScreenContainer>
      <View style={styles.wrap}>
        <View style={styles.greetingRow}>
          {member && <Avatar seed={member.id} label={displayName ?? member.email} size={44} />}
          <Text style={typography.title}>안녕하세요{displayName ? `, ${displayName}님` : ""}</Text>
        </View>

        <Pressable onPress={() => router.push("/workspaces")}>
          <Card style={styles.hubCard}>
            <View style={styles.hubCardRow}>
              <IconChip icon={<WorkspaceIcon size={20} color={colors.primary} />} />
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
              <IconChip icon={<SubscriptionIcon size={20} color={colors.primary} />} />
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
