import { useEffect, useState } from "react";
import { Text, View } from "react-native";
import { subscriptionApi } from "../../../api/subscription";
import { Card } from "../../../components/ui/Card";
import { EmptyState, Loading } from "../../../components/ui/Feedback";
import { ScreenContainer } from "../../../components/ui/ScreenContainer";
import { useTheme, useThemedStyles } from "../../../components/ui/ThemeContext";
import type { PaymentHistoryResponse } from "../../../types";

function formatDateTime(value: string | null) {
  return value ? new Date(value).toLocaleString("ko-KR") : "-";
}

const PAYMENT_STATUS_LABEL: Record<PaymentHistoryResponse["status"], string> = {
  PAID: "결제 성공",
  FAILED: "결제 실패",
};

export default function PaymentsScreen() {
  const { colors, typography } = useTheme();
  const styles = useThemedStyles((colors) => ({
    wrap: { gap: 12 },
    row: { flexDirection: "row" as const, justifyContent: "space-between" as const },
    paymentItem: { gap: 4, borderColor: colors.border },
  }));

  const [payments, setPayments] = useState<PaymentHistoryResponse[] | null>(null);

  useEffect(() => {
    subscriptionApi.getPayments().then(setPayments).catch(() => setPayments([]));
  }, []);

  return (
    <ScreenContainer>
      <View style={styles.wrap}>
        {payments === null ? (
          <Loading />
        ) : payments.length === 0 ? (
          <EmptyState label="결제 내역이 없습니다." />
        ) : (
          payments.map((p) => (
            <Card key={p.id} style={styles.paymentItem}>
              <View style={styles.row}>
                <Text style={typography.label}>{p.amount.toLocaleString("ko-KR")}원</Text>
                <Text style={p.status === "FAILED" ? { ...typography.muted, color: colors.danger } : typography.muted}>
                  {PAYMENT_STATUS_LABEL[p.status]}
                </Text>
              </View>
              <Text style={typography.muted}>{formatDateTime(p.paidAt ?? p.createdAt)}</Text>
            </Card>
          ))
        )}
      </View>
    </ScreenContainer>
  );
}
