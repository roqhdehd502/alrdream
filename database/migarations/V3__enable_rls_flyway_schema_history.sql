-- [보안] flyway_schema_history는 Flyway가 자체적으로 생성하는 테이블이라 V2에서는 대상에서 뺐지만,
-- 다른 테이블과 마찬가지로 Supabase Data API에 "UNRESTRICTED"로 노출되는 건 동일하다. 마이그레이션 이력이
-- 민감정보는 아니지만 일관성을 위해 나머지 테이블과 동일하게 RLS를 켠다 (정책 없음, FORCE 아님 —
-- V2와 동일한 이유로 backend의 JDBC 연결은 영향 없고 Data API만 차단된다).
ALTER TABLE flyway_schema_history ENABLE ROW LEVEL SECURITY;
