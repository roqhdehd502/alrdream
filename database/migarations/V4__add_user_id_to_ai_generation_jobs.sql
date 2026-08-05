-- [04_milestone.md] Phase 06 설계 결정: Job 상태 폴링 API(GET /api/ai-generation-jobs/{jobId})가 본인이 생성한
-- Job만 조회하도록 소유자 컬럼이 필요하다 ([03] §5 원안에는 없었음 — target_id가 다형 참조라 소유권을
-- target 테이블 조인으로 확인할 수 없어, Job 자신에 소유자를 직접 들고 있는 편이 단순하고 안전하다).
ALTER TABLE ai_generation_jobs ADD COLUMN user_id UUID NOT NULL REFERENCES users (id);

CREATE INDEX idx_ai_generation_jobs_user_id ON ai_generation_jobs (user_id);
