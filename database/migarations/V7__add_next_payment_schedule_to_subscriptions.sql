-- Phase 18 후속 — 구독 해지 시 PortOne에 예약된 다음 결제를 실제로 취소(revoke)하려면, 마지막으로
-- 등록한 결제 예약(paymentSchedule)의 ID를 알고 있어야 한다(PortOne SDK의 revokePaymentSchedules는
-- billingKey + scheduleId 목록을 받는다). paymentId/scheduleId는 매번 랜덤 생성되어 재계산이 불가능하므로
-- 별도 컬럼에 보관한다.
ALTER TABLE subscriptions
    ADD COLUMN next_payment_schedule_id TEXT;
