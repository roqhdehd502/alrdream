# AI 설문 설계

# 1. 설문의 역할

설문은 사용자 입력을 구조화된 데이터로 수집하여, AI가 기획안/설계 문서를 생성할 때 사용하는 **프롬프트 재료**다.

- 자유 텍스트 인터뷰 방식이 아니라, **정해진 문항(JSON)** 으로 수집한다 → AI 결과물의 형태를 일정하게 유지하기 위함
- 모든 문항은 `promptKey`를 가져, 답변이 프롬프트 템플릿의 변수로 그대로 매핑된다
- 응답은 그 자체로 보관되며(불변), 기획/설계 각 버전은 어떤 응답으로 생성됐는지를 참조로 가진다 ([01] 5, 11번 버전 관리 요구사항)

---

# 2. 설문이 필요한 시점

| 시점 | 설문 종류 | 비고 |
| --- | --- | --- |
| 워크스페이스 생성 — "아이템이 있음" | `PLANNING_HAS_IDEA` | [01] 2번 |
| 워크스페이스 생성 — "고민 중" | `PLANNING_EXPLORING` | [01] 2번 |
| 기획 수정 | 위 두 설문 중 해당 버전의 응답을 불러와 수정 | [01] 5번 |
| 분석 생성/수정 | **설문 없음** — 기획안 본문을 그대로 입력으로 사용 | [01] 7, 8번 |
| 설계 생성 | `DESIGN` | [01] 10번 |
| 설계 수정 | `DESIGN` 응답을 불러와 수정 | [01] 11번 |

---

# 3. 설문 정의 스키마 (Survey Schema)

문항 구조 자체를 표현하는 메타 스키마. 코드가 아닌 데이터로 관리하여, 문항이 바뀌어도 배포 없이 버전만 올려 대응할 수 있게 한다.

```json
{
  "surveyKey": "PLANNING_HAS_IDEA",
  "version": 1,
  "title": "사업 아이템 기획 설문",
  "questions": [
    {
      "id": "Q1",
      "promptKey": "idea_summary",
      "type": "LONG_TEXT",
      "question": "생각하고 계신 사업 아이디어를 한두 문장으로 설명해주세요.",
      "required": true,
      "allowUnknown": false
    },
    {
      "id": "Q6",
      "promptKey": "service_form",
      "type": "SINGLE_CHOICE",
      "question": "어떤 형태의 서비스를 생각하고 계신가요?",
      "required": true,
      "options": [
        { "key": "WEB", "label": "웹 서비스" },
        { "key": "APP", "label": "모바일 앱" },
        { "key": "SAAS", "label": "B2B SaaS" },
        { "key": "OFFLINE", "label": "오프라인/현장 서비스" },
        { "key": "ETC", "label": "기타" }
      ],
      "allowUnknown": false
    }
  ]
}
```

**필드 설명**

- `type`: `SINGLE_CHOICE` / `MULTI_CHOICE` / `SHORT_TEXT` / `LONG_TEXT` / `SCALE`(1~5) 중 하나
- `promptKey`: AI 프롬프트 템플릿에서 참조하는 변수명 (`{{idea_summary}}` 형태로 치환)
- `allowUnknown`: `true`면 "잘 모르겠어요" 응답을 허용 → 해당 항목은 기획안의 "리스크 & 보완 포인트" 섹션 생성 재료로 전달 ([01] 12-4)
- `options`: choice 계열 타입에서만 사용

---

# 4. 설문 응답 스키마 (Survey Response Schema)

사용자가 실제로 제출한 답변. 워크스페이스 및 기획/설계 버전과 연결되는 단위.

```json
{
  "responseId": "b3f1c8e0-...",
  "surveyKey": "PLANNING_HAS_IDEA",
  "surveyVersion": 1,
  "workspaceId": "8a2e...",
  "submittedAt": "2026-08-03T10:00:00+09:00",
  "answers": [
    {
      "questionId": "Q1",
      "type": "LONG_TEXT",
      "value": "야근이 잦은 직장인을 위한 저녁 배달 정기구독 서비스",
      "isUnknown": false
    },
    {
      "questionId": "Q6",
      "type": "SINGLE_CHOICE",
      "value": "APP",
      "isUnknown": false
    }
  ]
}
```

- `answers[].type`은 해당 설문 정의의 문항 타입과 항상 일치해야 한다 (검증 시 크로스체크)
- `isUnknown: true`인 항목은 `value`를 빈 값으로 두거나 생략 가능
- 응답은 수정 불가(immutable). "기획 수정"은 새 `responseId`를 만들고 기존 응답 값을 초기값으로 불러와 편집 후 재제출하는 방식 ([01] 5번)

---

# 5. 실제 설문 문항

## 5-1. `PLANNING_HAS_IDEA` — 사업 아이템이 있는 경우

| ID | promptKey | 타입 | 질문 | allowUnknown |
| --- | --- | --- | --- | --- |
| Q1 | idea_summary | LONG_TEXT | 아이디어를 한두 문장으로 설명해주세요 | N |
| Q2 | problem_definition | LONG_TEXT | 어떤 문제를 해결하려는 건가요? | N |
| Q3 | problem_frequency | SCALE | 그 문제는 얼마나 자주 발생하나요? (1: 가끔 ~ 5: 매일) | N |
| Q4 | existing_alternatives | LONG_TEXT | 지금까지는 이 문제를 어떻게 해결해왔나요? (기존 방법/도구) | Y |
| Q5 | target_customer | LONG_TEXT | 주 사용자는 누구인가요? 최대한 구체적으로 (예: "야근 많은 20~30대 직장인") | N |
| Q6 | service_form | SINGLE_CHOICE | 어떤 형태의 서비스를 생각하고 계신가요? (웹/앱/SaaS/오프라인/기타) | N |
| Q7 | competitors | LONG_TEXT | 알고 계신 경쟁 서비스나 비슷한 서비스가 있나요? | Y |
| Q8 | revenue_model_idea | MULTI_CHOICE | 예상하는 수익 모델은? (구독/광고/수수료/판매/모름) | Y |
| Q9 | available_resources | MULTI_CHOICE | 지금 활용 가능한 자원은? (개발 인력/자본/네트워크/도메인 지식/없음) | N |
| Q10 | risk_concerns | LONG_TEXT | 이 아이템에서 스스로 우려되거나 불확실하다고 느끼는 부분이 있나요? | Y |

→ Q2·Q3는 "문제 정의", Q4·Q7은 "경쟁/대체 분석", Q10은 "리스크 & 보완 포인트" 섹션 생성에 직접 대응 ([01] 12-4 구조).

## 5-2. `PLANNING_EXPLORING` — 아이템이 고민 중인 경우

아이디어가 없는 상태이므로, 사용자의 배경·자원·관심사를 파악해 AI가 아이템 자체를 제안하는 데 필요한 정보를 수집한다.

| ID | promptKey | 타입 | 질문 | allowUnknown |
| --- | --- | --- | --- | --- |
| Q1 | interested_fields | MULTI_CHOICE | 관심 있는 분야/산업군은? | N |
| Q2 | expertise | LONG_TEXT | 갖고 계신 전문성이나 경력을 알려주세요 | Y |
| Q3 | daily_pain_points | LONG_TEXT | 평소 생활/업무 중 불편하다고 느꼈던 것이 있나요? | Y |
| Q4 | available_resources | MULTI_CHOICE | 활용 가능한 자원은? (자본/인맥/시간/없음) | N |
| Q5 | preferred_business_type | SINGLE_CHOICE | 선호하는 사업 형태는? (온라인 서비스/오프라인/제품 판매/구독 서비스) | Y |
| Q6 | target_scale | SINGLE_CHOICE | 목표로 하는 규모는? (사이드 프로젝트/1인 기업/정식 스타트업) | N |
| Q7 | available_time_budget | SHORT_TEXT | 투입 가능한 시간과 예산을 대략 알려주세요 | Y |
| Q8 | risk_tolerance | SCALE | 리스크 감수 성향은? (1: 안정 지향 ~ 5: 공격적) | N |

## 5-3. `DESIGN` — 설계 설문

분석 결과(합법 여부/가용 리소스/경쟁 서비스)를 이미 확보한 상태에서, 실제 서비스 구조를 좁히기 위한 설문.

| ID | promptKey | 타입 | 질문 | allowUnknown |
| --- | --- | --- | --- | --- |
| Q1 | core_feature_priority | MULTI_CHOICE | 분석 단계에서 도출된 기능 후보 중, 우선순위가 높은 것을 골라주세요 | N |
| Q2 | mvp_scope | LONG_TEXT | 1차 출시(MVP)에 반드시 포함하고 싶은 범위는? | N |
| Q3 | tech_constraints | LONG_TEXT | 기술적/일정/예산 제약이 있다면 알려주세요 | Y |
| Q4 | platform_priority | SINGLE_CHOICE | 우선 출시할 플랫폼은? (웹/앱/웹+앱 동시) | N |
| Q5 | data_sensitivity | MULTI_CHOICE | 다루게 될 데이터 중 민감한 항목이 있나요? (개인정보/결제/위치/없음) | Y |

`core_feature_priority`의 `options`는 고정 목록이 아니라 **분석 단계 산출물에서 동적으로 생성**된다 (예: 분석 문서의 "핵심 기능 후보" 목록을 옵션으로 주입). 이 문항만 다른 문항과 달리 워크스페이스별로 옵션이 달라지는 **동적 설문 문항**이라는 점에 유의한다.

---

# 6. AI 프롬프트 매핑

- 답변 배열은 `{ [promptKey]: value }` 형태의 맵으로 변환되어 프롬프트 템플릿에 주입된다
- `isUnknown: true`인 답변은 값 대신 `"(사용자가 확신하지 못함 — AI가 조사/제안 필요)"` 같은 플래그 텍스트로 치환되어, 기획안 12-4의 "리스크 & 보완 포인트" 섹션에 자동으로 반영되도록 한다
- `DESIGN` 설문은 `PLANNING_*` 응답 + 분석 산출물까지 함께 프롬프트에 포함되어야 일관된 문서가 나온다 (설계는 이전 단계들의 컨텍스트를 누적해서 사용)

---

# 7. 버전 관리

- 설문 응답(`responseId`)은 불변이며, 기획/설계 각 버전 레코드는 자신을 생성한 `responseId`를 FK로 가진다
- "수정"은 새 응답을 만드는 것이지 기존 응답을 덮어쓰는 것이 아니다 → [01] 5, 11번의 "각 버전별로 어떤 설문 데이터를 작성했는지 기록" 요구사항을 응답 테이블만으로 자연스럽게 충족
- `surveyKey` + `surveyVersion`을 함께 저장해, 문항 구성이 나중에 바뀌어도 과거 응답을 정확히 재현/조회할 수 있게 한다
