-- [04_milestone.md] Phase 12 — Admin의 "AI 프롬프트 템플릿 관리"/"Free 티어 한도 조정" 기능을 위한 테이블.
-- 이전까지 기획/분석/설계 생성에 쓰이는 시스템 프롬프트·스키마는 Java 코드(PlanningGenerationSpec 등)에
-- 하드코딩돼 있었다. survey_definitions와 동일한 패턴(불변, 버전 발행)으로 DB로 옮겨 Admin에서 편집 가능하게 한다.

-- =========================================================
-- prompt_templates
-- =========================================================
CREATE TABLE prompt_templates (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prompt_type      VARCHAR(20) NOT NULL CHECK (prompt_type IN ('PLANNING', 'ANALYSIS', 'DESIGN')),
    version          INT         NOT NULL,
    tool_name        VARCHAR(255) NOT NULL,
    tool_description TEXT        NOT NULL,
    system_prompt    TEXT        NOT NULL,
    -- Claude Tool Use의 input_schema로 그대로 전달되는 JSON Schema — 문항 스키마와 달리 애플리케이션이
    -- 파싱하지 않고 그대로 문자열로 전달하므로 JSONB가 아닌 TEXT로 저장한다(survey_definitions.schema와의
    -- 차이 — 저기는 애플리케이션이 필드 단위로 역직렬화하지만 여긴 통째로 Claude에 전달만 한다).
    schema_json      TEXT        NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (prompt_type, version)
);

ALTER TABLE prompt_templates ENABLE ROW LEVEL SECURITY;

-- =========================================================
-- free_tier_settings — FREE 플랜 월별 AI 생성 횟수 한도. Admin이 조정할 수 있도록 단일 행으로 관리한다
-- ([01] 13번 BM, [03] §2-1 "구독/사용량 관리"). 기존에는 app.ai.free-tier-monthly-limit(application.yml)
-- 고정값이었던 것을 여기로 옮긴다 — 값 변경에 재배포가 필요 없게 하기 위함.
-- =========================================================
CREATE TABLE free_tier_settings (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    monthly_limit     INT         NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE free_tier_settings ENABLE ROW LEVEL SECURITY;

-- 기존 하드코딩 값을 초기 버전 1로 그대로 이관 — 마이그레이션 이후에도 생성 결과가 바뀌지 않는다.
INSERT INTO prompt_templates (prompt_type, version, tool_name, tool_description, system_prompt, schema_json) VALUES
('PLANNING', 1, 'emit_planning_document', '사용자의 설문 응답을 바탕으로 구조화된 사업 기획안을 반환한다.',
$SYS_PLANNING$당신은 사용자의 사업 아이디어를 실행 가능한 사업 기획서로 변환해주는 AI다.

입력으로 "promptKey: 답변" 형태의 설문 응답 목록이 주어진다. 답변 값이
"(사용자가 확신하지 못함 — AI가 조사/제안 필요)"로 표시된 항목은 사용자가 확신하지 못하는 부분이니,
일반적인 시장 정보와 상식을 바탕으로 조사/제안하고 이 사실을 "리스크 & 보완 포인트" 섹션에 반영하라.

다음 원칙을 반드시 지켜라.
- 모든 내용은 한국어로 작성한다.
- "아이디어 정리해드립니다" 수준의 막연한 요약이 아니라, "사업을 시작할 수 있는 수준"의 구체적이고
  실행 가능한 내용으로 작성한다.
- 타겟 고객은 "직장인"처럼 막연하게 쓰지 말고 "야근이 많은 20~30대 직장인"처럼 구체적으로 쓴다.
- 수익 모델은 현실성 있는 구조를 AI가 직접 제안해야 한다.
- MVP 전략은 반드시 포함한다 — 없으면 쓸모없는 문서가 된다.
- 실행 로드맵은 기획/개발/출시 등 단계별 액션을 포함한다.

주어진 도구(tool)를 호출해 구조화된 형태로만 응답하라.
$SYS_PLANNING$,
$SCHEMA_PLANNING${
  "type": "object",
  "properties": {
    "idea_summary": {
      "type": "object",
      "properties": {
        "one_line_pitch": {"type": "string", "description": "한 줄 아이디어"},
        "problem_to_solve": {"type": "string", "description": "해결하려는 문제"},
        "target_customer": {"type": "string", "description": "타겟 고객"},
        "core_value": {"type": "string", "description": "핵심 가치"}
      },
      "required": ["one_line_pitch", "problem_to_solve", "target_customer", "core_value"]
    },
    "problem_definition": {
      "type": "object",
      "properties": {
        "what": {"type": "string", "description": "어떤 문제가 있는가"},
        "why_important": {"type": "string", "description": "왜 중요한가"},
        "frequency": {"type": "string", "description": "얼마나 자주 발생하는가"}
      },
      "required": ["what", "why_important", "frequency"]
    },
    "target_customer_analysis": {
      "type": "object",
      "properties": {
        "main_users": {"type": "string", "description": "주요 사용자"},
        "characteristics": {"type": "string", "description": "사용자 특징"},
        "usage_scenario": {"type": "string", "description": "사용 시나리오"}
      },
      "required": ["main_users", "characteristics", "usage_scenario"]
    },
    "solution_proposal": {
      "type": "object",
      "properties": {
        "service_overview": {"type": "string", "description": "서비스 개요"},
        "core_features": {"type": "array", "items": {"type": "string"}, "description": "핵심 기능 3~5개"},
        "user_flow": {"type": "string", "description": "사용자 흐름 (간단)"}
      },
      "required": ["service_overview", "core_features", "user_flow"]
    },
    "service_structure": {
      "type": "object",
      "properties": {
        "platform_type": {"type": "string", "description": "웹 / 앱 / SaaS 형태"},
        "feature_blocks": {"type": "array", "items": {"type": "string"}, "description": "주요 기능 블록 (예: 로그인, 콘텐츠 생성, 저장, 공유)"}
      },
      "required": ["platform_type", "feature_blocks"]
    },
    "competitive_analysis": {
      "type": "object",
      "properties": {
        "existing_alternatives": {"type": "string", "description": "기존 해결 방법"},
        "competitors": {"type": "string", "description": "경쟁 서비스"},
        "differentiation": {"type": "string", "description": "차별점"}
      },
      "required": ["existing_alternatives", "competitors", "differentiation"]
    },
    "revenue_model": {
      "type": "object",
      "properties": {
        "how_to_earn": {"type": "string", "description": "어떻게 돈 버는지"},
        "feasibility": {"type": "string", "description": "현실성 있는 구조에 대한 설명"}
      },
      "required": ["how_to_earn", "feasibility"]
    },
    "mvp_strategy": {
      "type": "object",
      "properties": {
        "minimum_features": {"type": "array", "items": {"type": "string"}, "description": "최소 기능 정의"},
        "launch_scope": {"type": "string", "description": "1차 출시 범위"}
      },
      "required": ["minimum_features", "launch_scope"]
    },
    "execution_roadmap": {
      "type": "array",
      "description": "단계별 액션 (예: 1단계 기획, 2단계 개발, 3단계 출시)",
      "items": {
        "type": "object",
        "properties": {
          "stage": {"type": "string"},
          "actions": {"type": "string"}
        },
        "required": ["stage", "actions"]
      }
    },
    "risks_and_improvements": {
      "type": "object",
      "properties": {
        "weaknesses": {"type": "string", "description": "부족한 점"},
        "risks": {"type": "string", "description": "위험 요소"},
        "needs_further_research": {"type": "string", "description": "추가 조사 필요 영역 — \"모름\" 응답 활용"}
      },
      "required": ["weaknesses", "risks", "needs_further_research"]
    }
  },
  "required": [
    "idea_summary", "problem_definition", "target_customer_analysis", "solution_proposal",
    "service_structure", "competitive_analysis", "revenue_model", "mvp_strategy",
    "execution_roadmap", "risks_and_improvements"
  ]
}
$SCHEMA_PLANNING$);

INSERT INTO prompt_templates (prompt_type, version, tool_name, tool_description, system_prompt, schema_json) VALUES
('ANALYSIS', 1, 'emit_analysis_document', '기획안 본문을 바탕으로 구조화된 사업 분석을 반환한다.',
$SYS_ANALYSIS$당신은 사업 기획안을 검토해 실행 전 점검이 필요한 사항을 분석해주는 AI다.

입력으로 [01] 12-4 구조를 따르는 기획안 전체(JSON)가 주어진다. 이 내용을 바탕으로 아래 항목을
분석하라.
- 합법 여부: 이 사업을 한국에서 운영하는 데 법적 문제가 없는지, 필요한 인허가/신고 사항이 있는지.
- 가용 리소스: 이 사업을 시작하는 데 필요한 물적 자원(자본, 장비, 공간 등)과 인적 자원(필요 인력
  구성, 전문성)이 무엇이고 확보 가능성은 어떤지.
- 경쟁 서비스: 기획안에 언급된 경쟁/대체 서비스를 포함해 시장에 이미 존재하는 경쟁 구도와 이 사업이
  가질 수 있는 경쟁 우위.
- 핵심 기능 후보: 기획안의 솔루션 제안/서비스 구조를 바탕으로, 다음 설계 단계에서 우선순위를 정할
  구체적인 기능 후보 3~8개를 뽑아라. 각 후보는 영문 대문자 스네이크케이스 key(예: REALTIME_CHAT)와
  한국어 label로 표현한다.
- 종합 의견: 위 내용을 종합해 이 사업을 실행할 만한지에 대한 의견.

모든 내용은 한국어로 작성하고(핵심 기능 후보의 key는 예외), 막연한 이야기가 아니라 기획안 내용에
근거한 구체적인 분석이어야 한다. 주어진 도구(tool)를 호출해 구조화된 형태로만 응답하라.
$SYS_ANALYSIS$,
$SCHEMA_ANALYSIS${
  "type": "object",
  "properties": {
    "legality": {
      "type": "object",
      "properties": {
        "is_legal": {"type": "string", "description": "합법 여부 및 근거"},
        "considerations": {"type": "string", "description": "법적으로 유의해야 할 사항"},
        "required_licenses_or_permits": {"type": "string", "description": "필요한 인허가/신고 사항 (없으면 \"없음\")"}
      },
      "required": ["is_legal", "considerations", "required_licenses_or_permits"]
    },
    "resource_availability": {
      "type": "object",
      "properties": {
        "material_resources": {"type": "string", "description": "필요한 물적 자원(자본, 장비, 공간 등)과 확보 가능성"},
        "human_resources": {"type": "string", "description": "필요한 인적 자원(인력 구성, 전문성)과 확보 가능성"},
        "overall_feasibility": {"type": "string", "description": "종합적인 자원 확보 가능성 평가"}
      },
      "required": ["material_resources", "human_resources", "overall_feasibility"]
    },
    "competitive_landscape": {
      "type": "object",
      "properties": {
        "existing_competitors": {"type": "string", "description": "기존 경쟁/대체 서비스"},
        "market_saturation": {"type": "string", "description": "시장 포화도"},
        "competitive_advantage": {"type": "string", "description": "이 사업이 가질 수 있는 경쟁 우위"}
      },
      "required": ["existing_competitors", "market_saturation", "competitive_advantage"]
    },
    "core_feature_candidates": {
      "type": "array",
      "description": "설계 단계에서 우선순위를 정할 핵심 기능 후보 (3~8개)",
      "items": {
        "type": "object",
        "properties": {
          "key": {"type": "string", "description": "영문 대문자 스네이크케이스 식별자, 예: REALTIME_CHAT"},
          "label": {"type": "string", "description": "한국어 기능 이름"}
        },
        "required": ["key", "label"]
      }
    },
    "overall_assessment": {"type": "string", "description": "종합 의견"}
  },
  "required": [
    "legality", "resource_availability", "competitive_landscape",
    "core_feature_candidates", "overall_assessment"
  ]
}
$SCHEMA_ANALYSIS$);

INSERT INTO prompt_templates (prompt_type, version, tool_name, tool_description, system_prompt, schema_json) VALUES
('DESIGN', 1, 'emit_design_document', '기획/분석 결과와 설계 설문 응답을 바탕으로 구조화된 설계 문서를 반환한다.',
$SYS_DESIGN$당신은 사업 기획·분석 내용을 실제로 개발 가능한 서비스 설계 문서로 구체화해주는 AI다.

입력은 세 부분으로 주어진다.
1. [기획 단계 설문 응답]: 최초 사업 아이디어에 대한 사용자의 원본 답변 (promptKey: 값)
2. [분석 결과]: 합법성/가용 자원/경쟁 구도/핵심 기능 후보를 담은 이전 단계 분석 결과 (JSON)
3. [설계 설문 응답]: 사용자가 이번 설계 단계에서 고른 답변 — 우선순위 높은 핵심 기능(분석의 기능
   후보 중 선택), MVP 범위, 기술/일정/예산 제약, 우선 출시 플랫폼, 민감 데이터 여부

다음을 반영해 설계 문서를 작성하라.
- 사용자가 설계 설문에서 고른 핵심 기능들을 중심으로 각 기능의 구체적인 명세를 작성한다.
- MVP 범위는 사용자가 밝힌 범위를 존중하되, 실행 가능한 수준으로 구체화한다.
- 기술/일정/예산 제약을 반드시 반영해 현실적인 기술 스택과 아키텍처를 제안한다.
- 우선 출시 플랫폼에 맞는 구조를 제안한다.
- 민감 데이터가 있다고 답했다면 구체적인 보호 방안을 제시한다("없음"이면 "해당 없음"으로 짧게 적는다).
- 모든 내용은 한국어로 작성하고, 막연한 이야기가 아니라 실제 개발 착수가 가능한 수준으로 구체적이어야 한다.

주어진 도구(tool)를 호출해 구조화된 형태로만 응답하라.
$SYS_DESIGN$,
$SCHEMA_DESIGN${
  "type": "object",
  "properties": {
    "feature_specification": {
      "type": "array",
      "description": "설계 설문에서 우선순위로 선택된 핵심 기능별 상세 명세",
      "items": {
        "type": "object",
        "properties": {
          "feature": {"type": "string", "description": "기능 이름"},
          "description": {"type": "string", "description": "상세 설명 (동작 방식, 화면 구성 등)"},
          "priority": {"type": "string", "description": "우선순위 (예: 필수/권장/선택)"}
        },
        "required": ["feature", "description", "priority"]
      }
    },
    "mvp_definition": {
      "type": "object",
      "properties": {
        "scope": {"type": "string", "description": "MVP에 포함되는 범위"},
        "excluded_for_later": {"type": "string", "description": "MVP에서 제외하고 이후 반영할 항목"}
      },
      "required": ["scope", "excluded_for_later"]
    },
    "technical_architecture": {
      "type": "object",
      "properties": {
        "recommended_stack": {"type": "string", "description": "권장 기술 스택"},
        "architecture_overview": {"type": "string", "description": "아키텍처 개요"},
        "constraints_reflected": {"type": "string", "description": "사용자가 밝힌 기술/일정/예산 제약이 어떻게 반영됐는지"}
      },
      "required": ["recommended_stack", "architecture_overview", "constraints_reflected"]
    },
    "platform_plan": {
      "type": "object",
      "properties": {
        "primary_platform": {"type": "string", "description": "우선 출시 플랫폼"},
        "rationale": {"type": "string", "description": "선정 이유"}
      },
      "required": ["primary_platform", "rationale"]
    },
    "data_and_privacy": {
      "type": "object",
      "properties": {
        "sensitive_data_handled": {"type": "string", "description": "다루게 될 민감 데이터 (없으면 \"해당 없음\")"},
        "protection_measures": {"type": "string", "description": "보호 방안 (없으면 \"해당 없음\")"}
      },
      "required": ["sensitive_data_handled", "protection_measures"]
    },
    "system_structure": {
      "type": "array",
      "description": "화면/기능 블록 구조",
      "items": {"type": "string"}
    },
    "development_plan": {
      "type": "array",
      "description": "개발 단계별 계획",
      "items": {
        "type": "object",
        "properties": {
          "phase": {"type": "string"},
          "tasks": {"type": "string"}
        },
        "required": ["phase", "tasks"]
      }
    }
  },
  "required": [
    "feature_specification", "mvp_definition", "technical_architecture", "platform_plan",
    "data_and_privacy", "system_structure", "development_plan"
  ]
}
$SCHEMA_DESIGN$);

-- 기존 application.yml의 app.ai.free-tier-monthly-limit(5) 기본값을 그대로 이관.
INSERT INTO free_tier_settings (monthly_limit) VALUES (5);
