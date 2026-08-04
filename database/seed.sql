-- [02] §5 실제 설문 문항을 survey_definitions 시드로 삽입한다.
-- Flyway 마이그레이션과 별도로 관리 — 재실행해도 안전하도록 ON CONFLICT DO NOTHING을 사용한다.

-- =========================================================
-- PLANNING_HAS_IDEA ([02] §5-1)
-- =========================================================
INSERT INTO survey_definitions (survey_key, version, title, schema)
VALUES (
    'PLANNING_HAS_IDEA',
    1,
    '사업 아이템 기획 설문',
    $$
    {
      "surveyKey": "PLANNING_HAS_IDEA",
      "version": 1,
      "title": "사업 아이템 기획 설문",
      "questions": [
        { "id": "Q1", "promptKey": "idea_summary", "type": "LONG_TEXT", "question": "생각하고 계신 사업 아이디어를 한두 문장으로 설명해주세요.", "required": true, "allowUnknown": false },
        { "id": "Q2", "promptKey": "problem_definition", "type": "LONG_TEXT", "question": "어떤 문제를 해결하려는 건가요?", "required": true, "allowUnknown": false },
        { "id": "Q3", "promptKey": "problem_frequency", "type": "SCALE", "question": "그 문제는 얼마나 자주 발생하나요? (1: 가끔 ~ 5: 매일)", "required": true, "allowUnknown": false },
        { "id": "Q4", "promptKey": "existing_alternatives", "type": "LONG_TEXT", "question": "지금까지는 이 문제를 어떻게 해결해왔나요? (기존 방법/도구)", "required": true, "allowUnknown": true },
        { "id": "Q5", "promptKey": "target_customer", "type": "LONG_TEXT", "question": "주 사용자는 누구인가요? 최대한 구체적으로 (예: \"야근 많은 20~30대 직장인\")", "required": true, "allowUnknown": false },
        {
          "id": "Q6", "promptKey": "service_form", "type": "SINGLE_CHOICE",
          "question": "어떤 형태의 서비스를 생각하고 계신가요?", "required": true, "allowUnknown": false,
          "options": [
            { "key": "WEB", "label": "웹 서비스" },
            { "key": "APP", "label": "모바일 앱" },
            { "key": "SAAS", "label": "B2B SaaS" },
            { "key": "OFFLINE", "label": "오프라인/현장 서비스" },
            { "key": "ETC", "label": "기타" }
          ]
        },
        { "id": "Q7", "promptKey": "competitors", "type": "LONG_TEXT", "question": "알고 계신 경쟁 서비스나 비슷한 서비스가 있나요?", "required": true, "allowUnknown": true },
        {
          "id": "Q8", "promptKey": "revenue_model_idea", "type": "MULTI_CHOICE",
          "question": "예상하는 수익 모델은?", "required": true, "allowUnknown": true,
          "options": [
            { "key": "SUBSCRIPTION", "label": "구독" },
            { "key": "ADS", "label": "광고" },
            { "key": "COMMISSION", "label": "수수료" },
            { "key": "SALES", "label": "판매" },
            { "key": "UNKNOWN", "label": "모름" }
          ]
        },
        {
          "id": "Q9", "promptKey": "available_resources", "type": "MULTI_CHOICE",
          "question": "지금 활용 가능한 자원은?", "required": true, "allowUnknown": false,
          "options": [
            { "key": "DEV_TALENT", "label": "개발 인력" },
            { "key": "CAPITAL", "label": "자본" },
            { "key": "NETWORK", "label": "네트워크" },
            { "key": "DOMAIN_KNOWLEDGE", "label": "도메인 지식" },
            { "key": "NONE", "label": "없음" }
          ]
        },
        { "id": "Q10", "promptKey": "risk_concerns", "type": "LONG_TEXT", "question": "이 아이템에서 스스로 우려되거나 불확실하다고 느끼는 부분이 있나요?", "required": true, "allowUnknown": true }
      ]
    }
    $$::jsonb
)
ON CONFLICT (survey_key, version) DO NOTHING;

-- =========================================================
-- PLANNING_EXPLORING ([02] §5-2)
-- =========================================================
INSERT INTO survey_definitions (survey_key, version, title, schema)
VALUES (
    'PLANNING_EXPLORING',
    1,
    '사업 아이템 탐색 설문',
    $$
    {
      "surveyKey": "PLANNING_EXPLORING",
      "version": 1,
      "title": "사업 아이템 탐색 설문",
      "questions": [
        {
          "id": "Q1", "promptKey": "interested_fields", "type": "MULTI_CHOICE",
          "question": "관심 있는 분야/산업군은?", "required": true, "allowUnknown": false,
          "options": [
            { "key": "TECH_IT", "label": "IT/테크" },
            { "key": "COMMERCE", "label": "커머스/유통" },
            { "key": "FOOD", "label": "푸드/외식" },
            { "key": "EDUCATION", "label": "교육" },
            { "key": "HEALTHCARE", "label": "헬스케어/웰니스" },
            { "key": "TRAVEL_LEISURE", "label": "여행/레저" },
            { "key": "FINANCE", "label": "금융" },
            { "key": "LIFESTYLE_BEAUTY", "label": "라이프스타일/뷰티" },
            { "key": "CONTENT_MEDIA", "label": "콘텐츠/미디어" },
            { "key": "ETC", "label": "기타" }
          ]
        },
        { "id": "Q2", "promptKey": "expertise", "type": "LONG_TEXT", "question": "갖고 계신 전문성이나 경력을 알려주세요.", "required": true, "allowUnknown": true },
        { "id": "Q3", "promptKey": "daily_pain_points", "type": "LONG_TEXT", "question": "평소 생활/업무 중 불편하다고 느꼈던 것이 있나요?", "required": true, "allowUnknown": true },
        {
          "id": "Q4", "promptKey": "available_resources", "type": "MULTI_CHOICE",
          "question": "활용 가능한 자원은?", "required": true, "allowUnknown": false,
          "options": [
            { "key": "CAPITAL", "label": "자본" },
            { "key": "NETWORK", "label": "인맥" },
            { "key": "TIME", "label": "시간" },
            { "key": "NONE", "label": "없음" }
          ]
        },
        {
          "id": "Q5", "promptKey": "preferred_business_type", "type": "SINGLE_CHOICE",
          "question": "선호하는 사업 형태는?", "required": true, "allowUnknown": true,
          "options": [
            { "key": "ONLINE_SERVICE", "label": "온라인 서비스" },
            { "key": "OFFLINE", "label": "오프라인" },
            { "key": "PRODUCT_SALES", "label": "제품 판매" },
            { "key": "SUBSCRIPTION_SERVICE", "label": "구독 서비스" }
          ]
        },
        {
          "id": "Q6", "promptKey": "target_scale", "type": "SINGLE_CHOICE",
          "question": "목표로 하는 규모는?", "required": true, "allowUnknown": false,
          "options": [
            { "key": "SIDE_PROJECT", "label": "사이드 프로젝트" },
            { "key": "SOLOPRENEUR", "label": "1인 기업" },
            { "key": "STARTUP", "label": "정식 스타트업" }
          ]
        },
        { "id": "Q7", "promptKey": "available_time_budget", "type": "SHORT_TEXT", "question": "투입 가능한 시간과 예산을 대략 알려주세요.", "required": true, "allowUnknown": true },
        { "id": "Q8", "promptKey": "risk_tolerance", "type": "SCALE", "question": "리스크 감수 성향은? (1: 안정 지향 ~ 5: 공격적)", "required": true, "allowUnknown": false }
      ]
    }
    $$::jsonb
)
ON CONFLICT (survey_key, version) DO NOTHING;

-- =========================================================
-- DESIGN ([02] §5-3)
-- Q1(core_feature_priority)의 options는 정적 시드 데이터로 채우지 않는다 — [02] §5-3에 따라
-- 분석 단계 산출물에서 동적으로 생성되어 설문 조회 API 응답 시점에 주입된다.
-- =========================================================
INSERT INTO survey_definitions (survey_key, version, title, schema)
VALUES (
    'DESIGN',
    1,
    '서비스 설계 설문',
    $$
    {
      "surveyKey": "DESIGN",
      "version": 1,
      "title": "서비스 설계 설문",
      "questions": [
        {
          "id": "Q1", "promptKey": "core_feature_priority", "type": "MULTI_CHOICE",
          "question": "분석 단계에서 도출된 기능 후보 중, 우선순위가 높은 것을 골라주세요.", "required": true, "allowUnknown": false,
          "options": []
        },
        { "id": "Q2", "promptKey": "mvp_scope", "type": "LONG_TEXT", "question": "1차 출시(MVP)에 반드시 포함하고 싶은 범위는?", "required": true, "allowUnknown": false },
        { "id": "Q3", "promptKey": "tech_constraints", "type": "LONG_TEXT", "question": "기술적/일정/예산 제약이 있다면 알려주세요.", "required": true, "allowUnknown": true },
        {
          "id": "Q4", "promptKey": "platform_priority", "type": "SINGLE_CHOICE",
          "question": "우선 출시할 플랫폼은?", "required": true, "allowUnknown": false,
          "options": [
            { "key": "WEB", "label": "웹" },
            { "key": "APP", "label": "앱" },
            { "key": "WEB_AND_APP", "label": "웹+앱 동시" }
          ]
        },
        {
          "id": "Q5", "promptKey": "data_sensitivity", "type": "MULTI_CHOICE",
          "question": "다루게 될 데이터 중 민감한 항목이 있나요?", "required": true, "allowUnknown": true,
          "options": [
            { "key": "PERSONAL_INFO", "label": "개인정보" },
            { "key": "PAYMENT", "label": "결제" },
            { "key": "LOCATION", "label": "위치" },
            { "key": "NONE", "label": "없음" }
          ]
        }
      ]
    }
    $$::jsonb
)
ON CONFLICT (survey_key, version) DO NOTHING;
