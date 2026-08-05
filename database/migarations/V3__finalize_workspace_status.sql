-- [04_milestone.md] Phase 04 — V1__initial_schema.sql이 남겨둔 "상태 값 종류는 Phase 04에서 확정" 주석을 해소한다.
-- [01] 문서 어디에도 ACTIVE 외의 워크스페이스 레벨 상태 전이가 없고(소프트 삭제는 deleted_at으로 별도 관리),
-- 다른 테이블의 상태 컬럼과 동일하게 CHECK로 허용 값을 명시한다. 향후 상태가 늘어나면 이 제약을 확장한다.
ALTER TABLE workspaces
    ADD CONSTRAINT workspaces_status_check CHECK (status IN ('ACTIVE'));
