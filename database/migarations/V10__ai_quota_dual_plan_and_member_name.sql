-- [04_milestone.md] Phase 20 — (1) AI 생성 횟수 한도를 FREE/PRO 모두에 적용하도록 기획 변경(기존엔 PRO가
-- 무제한이었다). free_tier_settings의 단일 monthly_limit 컬럼을 plan별 컬럼 둘로 나눈다 — 이 테이블은
-- 애플리케이션이 갱신만 하는 단일 행 설정 테이블이라(V5 참고) rename도 그 한 행에만 적용된다.
ALTER TABLE free_tier_settings RENAME COLUMN monthly_limit TO free_monthly_limit;
ALTER TABLE free_tier_settings ADD COLUMN pro_monthly_limit INT NOT NULL DEFAULT 10;
UPDATE free_tier_settings SET free_monthly_limit = 1;

-- (2) 회원 이름 — 옵셔널, 기본값은 이메일(표시 로직은 프론트에서 계산). NULL이면 "이름을 설정하지 않음"을
-- 뜻하고, 화면에는 이메일 앞부분으로 대체해 보여준다.
ALTER TABLE users ADD COLUMN name VARCHAR(50);
