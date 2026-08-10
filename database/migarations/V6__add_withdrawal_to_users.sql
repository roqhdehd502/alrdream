-- [04_milestone.md] Phase 16 — 회원 탈퇴. 워크스페이스 등은 소프트 삭제(deleted_at)가 있지만 회원 자체를
-- 탈퇴시키는 수단이 없었다. users는 subscriptions/payment_history/workspaces 등에서 FK로 참조돼(§5) 하드
-- 삭제가 불가능하고, 결제 이력(payment_history)은 세무/분쟁 대응을 위해 보존해야 하므로 소프트 삭제 +
-- 개인정보 익명화 방식을 쓴다. withdrawn_at이 NULL이 아니면 로그인이 차단된다(AuthService).
ALTER TABLE users ADD COLUMN withdrawn_at TIMESTAMPTZ;
