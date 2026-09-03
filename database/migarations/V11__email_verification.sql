-- [04_milestone.md] Phase 23 — 이메일 인증. LOCAL 가입은 미인증으로 시작한다. OAuth(Google/Apple)는
-- 이미 공급자가 이메일을 검증했으므로 가입 즉시 인증됨으로 간주해 소급 반영한다. 기존 LOCAL 회원은
-- 인증되지 않은 상태로 남아 마이페이지에서 별도로 인증해야 한다(요구사항과 일치).
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT false;
UPDATE users SET email_verified = true WHERE provider <> 'LOCAL';
