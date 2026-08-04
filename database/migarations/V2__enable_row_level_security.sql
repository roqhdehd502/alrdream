-- [보안] Supabase는 테이블마다 PostgREST 기반 Data API를 자동 생성하며, RLS가 꺼진 테이블은
-- anon/authenticated 키만 있으면 누구나 HTTP로 직접 읽고 쓸 수 있다 (Table Editor의 "UNRESTRICTED" 경고).
--
-- 이 프로젝트는 [03] §5에 명시한 대로 Supabase Auth/클라이언트를 쓰지 않고, backend(Spring Boot)가
-- Session Pooler를 통해 테이블 소유자 권한(postgres.<project-ref>)으로 JDBC 직접 연결한다.
-- PostgreSQL은 기본적으로 테이블 소유자에게는 RLS를 적용하지 않으므로(FORCE ROW LEVEL SECURITY를 걸지 않는 한),
-- 아래처럼 RLS만 켜고 정책을 하나도 두지 않으면:
--   - backend(JDBC, 소유자 권한) 접근은 전혀 영향 없음
--   - Data API(anon/authenticated, 소유자 아님)는 허용 정책이 없어 완전히 차단됨
-- 즉 Data API를 별도로 끄지 않고도 RLS만으로 동일한 효과를 낸다. 향후 Admin/Frontend가 Supabase Data API를
-- 직접 쓰게 될 경우에만 명시적인 정책을 추가하면 된다.

ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE workspaces ENABLE ROW LEVEL SECURITY;
ALTER TABLE survey_definitions ENABLE ROW LEVEL SECURITY;
ALTER TABLE survey_responses ENABLE ROW LEVEL SECURITY;
ALTER TABLE planning_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE analysis_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE design_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_generation_jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE usage_quotas ENABLE ROW LEVEL SECURITY;
