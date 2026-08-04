-- [03] §5 데이터베이스 설계 기준 초기 스키마.
-- gen_random_uuid()는 PostgreSQL 13 미만에서 pgcrypto 확장이 필요해 명시적으로 활성화한다 (Supabase는 대체로 이미 활성화되어 있으나 idempotent라 안전).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- users
-- =========================================================
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    provider      VARCHAR(20)  NOT NULL CHECK (provider IN ('LOCAL', 'GOOGLE', 'APPLE')),
    provider_id   VARCHAR(255),
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    plan          VARCHAR(20)  NOT NULL DEFAULT 'FREE' CHECK (plan IN ('FREE', 'PRO')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (provider, provider_id)
);

-- =========================================================
-- workspaces
-- =========================================================
CREATE TABLE workspaces (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users (id),
    name       VARCHAR(255) NOT NULL,
    -- 상태 값 종류는 Phase 04(워크스페이스 도메인)에서 확정 — 우선 기본값만 둔다.
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_workspaces_user_id ON workspaces (user_id);

-- =========================================================
-- survey_definitions
-- =========================================================
CREATE TABLE survey_definitions (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    survey_key VARCHAR(50)  NOT NULL CHECK (survey_key IN ('PLANNING_HAS_IDEA', 'PLANNING_EXPLORING', 'DESIGN')),
    version    INT          NOT NULL,
    title      VARCHAR(255) NOT NULL,
    -- [02] §3 설문 정의 스키마 그대로 저장 (암호화 대상 아님 — Admin이 작성하는 문항 구조일 뿐 사용자 사업 데이터가 아님)
    schema     JSONB        NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (survey_key, version)
);

-- =========================================================
-- survey_responses
-- =========================================================
CREATE TABLE survey_responses (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    survey_definition_id  UUID        NOT NULL REFERENCES survey_definitions (id),
    workspace_id          UUID        NOT NULL REFERENCES workspaces (id),
    -- [03] §5 암호화 대상. AES-256으로 암호화된 [02] §4 응답 JSON을 텍스트로 저장하므로 JSONB가 아닌 TEXT를 쓴다
    -- (암호문은 유효한 JSON이 아니라 JSONB 컬럼에 담을 수 없음 — 역직렬화/검증은 애플리케이션의 AttributeConverter가 담당).
    answers               TEXT        NOT NULL,
    submitted_at          TIMESTAMPTZ NOT NULL DEFAULT now()
    -- 불변 레코드([02] §4) — updated_at 없음, UPDATE는 애플리케이션에서 금지
);

CREATE INDEX idx_survey_responses_workspace_id ON survey_responses (workspace_id);
CREATE INDEX idx_survey_responses_survey_definition_id ON survey_responses (survey_definition_id);

-- =========================================================
-- planning_versions
-- =========================================================
CREATE TABLE planning_versions (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id       UUID        NOT NULL REFERENCES workspaces (id),
    survey_response_id UUID        NOT NULL REFERENCES survey_responses (id),
    version_no         INT         NOT NULL,
    -- 암호화된 콘텐츠 — survey_responses.answers와 동일한 이유로 TEXT
    content            TEXT,
    status             VARCHAR(20) NOT NULL DEFAULT 'GENERATING' CHECK (status IN ('GENERATING', 'COMPLETED', 'FAILED')),
    deleted_at         TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (workspace_id, version_no)
);

CREATE INDEX idx_planning_versions_workspace_id ON planning_versions (workspace_id);

-- =========================================================
-- analysis_versions
-- =========================================================
CREATE TABLE analysis_versions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    planning_version_id UUID        NOT NULL REFERENCES planning_versions (id),
    version_no          INT         NOT NULL,
    content             TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'GENERATING' CHECK (status IN ('GENERATING', 'COMPLETED', 'FAILED')),
    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (planning_version_id, version_no)
);

CREATE INDEX idx_analysis_versions_planning_version_id ON analysis_versions (planning_version_id);

-- =========================================================
-- design_versions
-- =========================================================
CREATE TABLE design_versions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    analysis_version_id UUID        NOT NULL REFERENCES analysis_versions (id),
    survey_response_id  UUID        NOT NULL REFERENCES survey_responses (id),
    version_no          INT         NOT NULL,
    content             TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'GENERATING' CHECK (status IN ('GENERATING', 'COMPLETED', 'FAILED')),
    deleted_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (analysis_version_id, version_no)
);

CREATE INDEX idx_design_versions_analysis_version_id ON design_versions (analysis_version_id);

-- =========================================================
-- documents
-- =========================================================
CREATE TABLE documents (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- source_id는 source_type에 따라 planning_versions/analysis_versions/design_versions 중 하나를 가리키는 다형 참조라 DB FK를 걸 수 없다.
    source_type  VARCHAR(20)  NOT NULL CHECK (source_type IN ('PLANNING', 'ANALYSIS', 'DESIGN')),
    source_id    UUID         NOT NULL,
    file_url     VARCHAR(500) NOT NULL,
    generated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_documents_source ON documents (source_type, source_id);

-- =========================================================
-- ai_generation_jobs
-- =========================================================
CREATE TABLE ai_generation_jobs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- target_id도 documents.source_id와 동일한 이유로 다형 참조 (DB FK 없음).
    target_type   VARCHAR(20) NOT NULL CHECK (target_type IN ('PLANNING', 'ANALYSIS', 'DESIGN')),
    target_id     UUID        NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    error_message TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Job 상태 폴링 API([03] §4-4)가 target 기준으로 최신 job을 조회하는 패턴을 지원
CREATE INDEX idx_ai_generation_jobs_target ON ai_generation_jobs (target_type, target_id);

-- =========================================================
-- subscriptions
-- =========================================================
CREATE TABLE subscriptions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users (id),
    plan            VARCHAR(20) NOT NULL CHECK (plan IN ('FREE', 'PRO')),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PAST_DUE', 'CANCELED')),
    -- PortOne 빌링키 — 암호화 대상 [03] §4-7
    billing_key     TEXT,
    next_billing_at TIMESTAMPTZ,
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_subscriptions_user_id ON subscriptions (user_id);

-- =========================================================
-- payment_history
-- =========================================================
CREATE TABLE payment_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID        NOT NULL REFERENCES subscriptions (id),
    -- PortOne payment_id — 웹훅 멱등 처리 키([03] §4-7)
    payment_id      VARCHAR(255) NOT NULL UNIQUE,
    -- KRW는 소수 단위가 없어 정수로 저장
    amount          BIGINT      NOT NULL,
    status          VARCHAR(20) NOT NULL CHECK (status IN ('PAID', 'FAILED')),
    paid_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_history_subscription_id ON payment_history (subscription_id);

-- =========================================================
-- usage_quotas
-- =========================================================
CREATE TABLE usage_quotas (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID        NOT NULL REFERENCES users (id),
    -- 'YYYY-MM' 형식
    period            VARCHAR(7)  NOT NULL,
    generation_count  INT         NOT NULL DEFAULT 0,
    limit_count       INT         NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, period)
);
