-- ⚠️ 개발/테스트 전용 계정 시드. 절대 운영(production) DB에서 실행하지 않는다 —
-- 아래 비밀번호가 이 파일에 그대로 노출되어 있어, 운영 DB에 삽입되면 심각한 보안 사고로 이어진다.
-- role(USER/ADMIN)별 인증 동작 테스트(로그인, /api/admin/** 접근 제어 등)를 위한 용도.
-- 여러 번 실행해도 안전하도록 ON CONFLICT (email) DO NOTHING을 사용한다.
--
-- 아래 password_hash는 BCryptPasswordEncoder(work factor 10)로 생성한 해시다.

INSERT INTO users (email, password_hash, provider, role, plan)
VALUES
    ('admin@alrdream.test', '$2a$10$kil.IYtY3ISuG6nUZk3UpejlwfB1YkvNFcl8yAfREd7q29wRdgJXO', 'LOCAL', 'ADMIN', 'FREE'),
    ('user@alrdream.test', '$2a$10$kil.IYtY3ISuG6nUZk3UpejlwfB1YkvNFcl8yAfREd7q29wRdgJXO', 'LOCAL', 'USER', 'FREE')
ON CONFLICT (email) DO NOTHING;
