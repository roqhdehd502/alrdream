# 설계

# 1. 시스템 구성 개요

```
[Frontend (사용자, React/Expo — 웹+앱)]     [Admin (운영자, React 웹)]
              │                                      │
              └───────────────┬──────────────────────┘
                               ▼
                   [Backend (Spring Boot API)]
                               │
        ┌──────────────┬───────┴───────┬──────────────┐
        ▼              ▼               ▼              ▼
 [Supabase Postgres] [Redis]      [Claude API]   [Supabase Storage]
   (도메인 데이터)  (캐시/세션)   (기획/분석/설계    (생성된 PDF)
                                  콘텐츠 생성)
```

- **Frontend**: 사용자가 워크스페이스를 만들고 설문에 답하며 기획/분석/설계 산출물을 열람하는 앱
- **Admin**: 운영자가 설문 정의·사용자·구독·AI 프롬프트 템플릿을 관리하는 웹 콘솔
- **Backend**: 단일 Spring Boot API 서버. 도메인 로직, AI 연동, PDF 생성을 모두 담당 (별도 AI 서버 없음)
- **Database/Storage**: 동일 Supabase 프로젝트의 PostgreSQL + Storage를 함께 사용 (별도 클라우드 계정 관리 불필요)
- LLM Provider는 `AiClient` 인터페이스로 추상화하되, 1차 구현체는 **Anthropic Claude API**로 확정 (§4-3)
- Pro 구독 결제는 **포트원(PortOne) V2 API**(PG사: 토스페이먼츠 신모듈)로 처리하며, 결제 상태는 웹훅으로 동기화한다 (§4-7)

---

# 2. Admin 설계

## 2-1. 목적

Admin은 아래 항목을 관리한다. 특히 설문 정의 관리는 [02] 문서에서 정의한 "배포 없이 버전만 올려 문항을 교체"할 수 있어야 한다는 요구를 실제로 충족시키는 화면이다.

| 영역                     | 기능                                                                                                                                                                                                  |
| ------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 설문 정의 관리           | `survey_definitions` CRUD, 새 버전 발행, 미리보기                                                                                                                                                     |
| 사용자/워크스페이스 조회 | CS 대응용 조회, 상태 확인. Phase 19부터 Pro 일괄 지급/연장, 강제 Free 전환, 계정 제재(일시/영구) 액션 추가 — `ROLE_ADMIN` 대상은 실수로 관리자 콘솔 전체가 잠기는 사고를 막기 위해 제재 대상에서 제외 |
| AI 프롬프트 템플릿 관리  | `promptKey` → 프롬프트 템플릿 매핑 편집, 버전 관리                                                                                                                                                    |
| 구독 관리                | Pro 구독 현황(목록/상태별 분포 차트), 가격/프로모션 설정([01] 13번 BM) — "구독 관리" 페이지                                                                                                           |
| AI 프롬프트 횟수 관리    | FREE 티어 월별 생성 횟수 한도 조정 — 별도 페이지                                                                                                                                                      |
| 쿠폰 관리(Phase 19)      | 쿠폰 코드 생성/비활성화, 유저별 사용 현황 조회                                                                                                                                                        |
| 대시보드                 | 가입자/생성량/결제 현황 요약(스탯 타일 + 막대 차트)                                                                                                                                                   |

## 2-2. 기술 스택 및 구조

React + Vite + TypeScript (크로스플랫폼 불필요 — 웹 전용이므로 Frontend와 별도 스택으로 가볍게 구성)

```
admin/
├── src
│   ├── pages           # SurveyList, SurveyEditor, Users, Workspaces, Subscriptions, Dashboard
│   ├── features         # 화면별 로직 (설문 버전 발행, 프롬프트 편집 등)
│   ├── components        # 공통 UI (Table, Form, Modal)
│   ├── api               # Backend Admin API 클라이언트
│   ├── hooks
│   └── types
```

- 인증은 Frontend와 동일 백엔드를 쓰되, `role = ADMIN` 사용자만 Admin API에 접근 가능하도록 Spring Security에서 분리 (§4-5)

---

# 3. Frontend (사용자 앱) 설계

## 3-1. 화면 흐름 ([01] 기준)

```
워크스페이스 목록
  └─ [새 워크스페이스] → 분기 선택 (아이템 있음 / 고민 중)
                            → 설문 (PLANNING_HAS_IDEA | PLANNING_EXPLORING)
                            → AI 생성 대기 화면 (폴링/실시간 상태)
                            → 워크스페이스 상세로 이동

워크스페이스 상세
  ├─ 기획 탭  : 버전 목록 → 상세 → [수정] [삭제] [분석 시작]
  ├─ 분석 탭  : 버전 목록 → 상세 (합법여부/리소스/경쟁분석 포함) → [수정] [삭제] [설계 시작]
  ├─ 설계 탭  : 버전 목록 → 상세 → [수정] [삭제] [PDF 다운로드]
  └─ 설정 탭  : 워크스페이스 이름 수정 / 삭제
```

- "AI 생성 대기 화면"이 필요한 이유: 기획/분석/설계 생성은 LLM 호출 + PDF 렌더링이 포함돼 수 초~수십 초가 걸릴 수 있음 → 동기 응답 대신 작업(Job) 상태를 폴링하는 UX가 필요 (§4-4)

**내비게이션 구조(Phase 19)**: 앱다운 느낌을 위해 Apple HIG 스타일 하단 탭바(`expo-router`의 `Tabs`)를
도입했다 — 홈(허브, 향후 기능 추가를 대비해 카드 목록으로 구성)/워크스페이스/구독/마이페이지 4탭.
`(app)/(tabs)/` 그룹으로 묶고, 워크스페이스 상세·생성·결제 내역·쿠폰 등록처럼 깊이 들어가는 화면은
탭 그룹 밖의 형제 라우트로 둬 눌렀을 때 탭바 없이 전체 화면으로 푸시되는 iOS 드릴인 동작을 그대로
가져간다. 마이페이지에 "이번 달 AI 생성 사용량"(예전엔 구독 화면에 있었음)과 쿠폰 등록 진입점을,
구독 화면에는 "결제 내역 보기"로 분리된 별도 화면을 연결했다.

## 3-2. 크로스플랫폼 전략 (제안)

가이드라인의 "React 기반 크로스플랫폼 + 러닝커브 + 코드푸시 대응"을 만족하는 조합으로 **Expo (React Native) + Expo Router + react-native-web**을 제안한다.

- 하나의 코드베이스로 iOS / Android / Web 빌드
- **EAS Update**로 OTA 업데이트 (Microsoft App Center CodePush는 서비스 종료 수순이라 대체재로 EAS Update 채택)
- Expo가 네이티브 모듈 설정을 추상화해주므로 순수 React Native 대비 러닝커브가 낮음

```
frontend/
├── app/                  # Expo Router (파일 기반 라우팅)
│   ├── (workspace)/
│   ├── (auth)/
│   └── _layout.tsx
├── src
│   ├── features           # 워크스페이스, 설문, 기획/분석/설계 뷰어
│   ├── components
│   ├── api                 # Backend API 클라이언트 (React Query 등으로 캐싱/폴링)
│   ├── store                # 설문 진행 중 임시 상태 등
│   └── types                # [02] 설문 스키마 타입 (Survey/SurveyResponse)
```

## 3-3. 설문 렌더링

[02]의 설문 정의 스키마(`type`, `options`, `allowUnknown`)를 그대로 받아 문항 타입별 공용 컴포넌트(`SingleChoiceField`, `MultiChoiceField`, `TextField`, `ScaleField`)로 렌더링하는 **설문 엔진** 방식을 쓴다. 즉 문항이 늘어나거나 바뀌어도 프론트 배포 없이 Admin에서 발행한 새 버전이 그대로 반영된다.

---

# 4. Backend 설계

## 4-1. 도메인 모듈 구성

가이드라인의 DDD 스타일 구조를 실제 도메인에 맞게 구성한다.

```
backend/src/main/java/com/alrdream
├── global
│   ├── config
│   ├── error
│   ├── jpa                # BaseEntity/SoftDeleteBaseEntity, JPA Auditing 설정
│   └── security
├── domain
│   ├── member            # 사용자, 인증
│   ├── workspace          # 워크스페이스
│   ├── survey              # 설문 정의(survey_definitions) + 응답(survey_responses)
│   ├── planning             # 기획 버전
│   ├── analysis              # 분석 버전
│   ├── design                  # 설계 버전
│   ├── document                # 생성된 PDF 메타데이터
│   └── subscription              # 구독/결제(PortOne)/사용량 쿼터
│       ├── api / application / domain / infrastructure   (각 도메인 공통)
└── infrastructure
    ├── ai                 # LLM Client, PromptBuilder ([02] promptKey 매핑)
    ├── pdf                 # 콘텐츠(JSON) → PDF 렌더링
    ├── payment              # PortOne 클라이언트, 웹훅 서명 검증
    ├── redis                # 세션/캐시, AI 생성 Job 상태
    └── storage                # Object Storage 연동 (생성 PDF 업로드)
```

## 4-2. 도메인 간 관계 요약

| 도메인     | 참조                                                 | 비고                                |
| ---------- | ---------------------------------------------------- | ----------------------------------- |
| `planning` | `workspace`, `survey_response`                       | 기획은 설문 응답 1건에서 생성       |
| `analysis` | `planning` (특정 버전)                               | 별도 설문 없음, 기획 본문이 곧 입력 |
| `design`   | `analysis` (특정 버전), `survey_response`            | 분석 결과 + 설계 설문을 함께 입력   |
| `document` | `planning`/`analysis`/`design` 중 하나 (polymorphic) | 버전별 PDF 산출물                   |

## 4-3. AI 연동 구조

```
Controller → UseCase → PromptBuilder → AiClient(interface) → LLM Provider
                              │
                    [02] survey_response.answers
                    + 이전 단계 산출물(분석은 기획 본문, 설계는 분석+설문)
                              │
                              ▼
                     구조화된 JSON 응답 파싱
                    (12-4 PDF 구조의 섹션별 필드)
                              │
                              ▼
                    planning/analysis/design.content 저장
```

- `AiClient`는 인터페이스로 두고, 1차 구현체는 **Anthropic Claude API**로 확정 (추후 Provider 추가/교체 가능하도록 인터페이스는 유지)
- LLM에는 자유 텍스트가 아니라 **섹션별 JSON 스키마로 응답하도록 강제**한다 → Claude의 Tool Use(강제 tool-call)를 활용해 Structured Output 확보, PDF 렌더링 시 섹션 매핑이 안정적
- 외부 LLM API 호출은 Spring 6의 `@HttpExchange` 선언형 클라이언트로 구현 (가이드라인 1-2 대응)

## 4-4. 비동기 생성 처리

기획/분석/설계 생성은 시간이 걸리므로 즉시 응답 대신 Job 방식으로 처리한다.

1. `POST /planning` → `ai_generation_jobs` 레코드 생성(`PENDING`) 후 즉시 `jobId` 반환
2. Virtual Thread 기반 비동기 처리로 LLM 호출 → 완료 시 `planning_versions` 저장, Job `DONE` 처리
3. Frontend는 `GET /jobs/{jobId}`를 폴링 (또는 SSE)하여 완료 시 상세 화면으로 전환

## 4-5. 인증

- Spring Security 6 + JWT (Access/Refresh)
- 로그인 방식: 자체 회원가입/로그인(이메일+비밀번호) + OAuth2 소셜 로그인(Google, Apple)
  - `users.provider`를 `LOCAL | GOOGLE | APPLE | ...`로 두어, 이후 Provider가 추가돼도 컬럼/로직 구조 변경 없이 값만 늘어나도록 설계
  - Apple 로그인은 iOS 앱스토어 심사 정책상(소셜 로그인 제공 시 Apple 로그인 필수) 요구되는 항목이라 초기부터 포함
  - (2026-08-06) Apple Developer 유료 멤버십 구독 이슈로 자격증명 발급이 막혀 Apple 로그인 활성화는 잠정 보류. 코드/스키마는 그대로 유지하고 Google 로그인만 우선 배선. iOS 앱스토어 제출 전에는 위 정책 때문에 재개 필요 (자세한 경과는 `04_milestone.md` Phase 00/06 참고)
  - **구현 방식**: Spring Security의 OAuth2Client(브라우저 리다이렉트 기반) 대신, Frontend가 각 provider ID 토큰을 발급받아 백엔드가 검증하는 방식을 쓴다(`POST /api/auth/oauth/{google|apple}`) — 백엔드는 서명·발급자·audience만 검증
    - Google은 실제로는 `expo-auth-session`(범용 브라우저 기반 OAuth, Web application 타입 클라이언트) 구현이다. Google이 Web 타입 클라이언트에는 `https://`/`http://localhost` 리디렉션만 허용하고 `alrdream://` 같은 커스텀 스킴은 등록 자체를 거부해, **현재는 웹 빌드(Vercel)에서만 Google 로그인이 동작**하고 EAS로 빌드된 네이티브 앱에서는 버튼 자체를 숨긴다. 네이티브 지원이 필요해지면 `@react-native-google-signin/google-signin`(iOS/Android 전용 OAuth 클라이언트 + EAS 개발 빌드 필요, Expo Go 불가)으로 별도 구현해야 한다
- `role: USER | ADMIN` 클레임으로 Admin API 접근을 분리 (별도 Admin 전용 서버 없이 하나의 백엔드에서 권한만 분리)

## 4-6. PDF 생성

[01] 12번의 "AI 모델을 통해 PDF로 추출"은 LLM이 PDF를 직접 만든다는 의미가 아니라, **LLM의 구조화된 JSON 산출물을 정해진 템플릿으로 렌더링**하는 파이프라인으로 해석한다.

```
content(JSON) → Thymeleaf 템플릿(HTML, 12-4 구조 그대로) → OpenHTMLtoPDF → Supabase Storage 업로드
```

- Node/헤드리스 브라우저(Puppeteer) 의존 없이 순수 Java로 처리 가능한 **OpenHTMLtoPDF**를 제안 (Docker 이미지 단순화)
- Supabase Storage는 S3 호환 API를 제공하므로 AWS SDK S3 클라이언트를 그대로 사용해 업로드/서명 URL 발급 처리

## 4-7. 결제(구독) 연동 — PortOne V2

PG사는 **토스페이먼츠(신모듈)**, 결제 게이트웨이는 **포트원(PortOne) V2**로 확정. Pro 구독은 카드/퀵계좌이체 **빌링키** 방식의 정기결제로 구현한다.

```
[Frontend/Admin] PortOne SDK: requestIssueBillingKey()
        │  (카드정보는 PG사로 직접 전달, 백엔드/포트원 서버에 남지 않음)
        ▼
   billingKeyId 수신 → 백엔드로 전달
        ▼
[Backend] billingKeyId 암호화 저장 (subscriptions.billing_key)
        │
        ├─ 최초 결제 즉시 요청 (POST /payments)
        │
        └─ 다음 달 결제를 "예약결제"로 등록 (timeToPay 지정)
                   │
                   ▼  (지정 시각에 포트원 서버가 자동 결제 실행 — 자체 스케줄러 불필요)
        [PortOne] 웹훅 발송 → POST /webhooks/portone
                   │
        ┌──────────┴──────────┐
        ▼                     ▼
  Transaction.Paid      Transaction.Failed
  → payment_history 기록 → payment_history 기록
  → 다음 달 결제 재예약    → subscription.status = PAST_DUE
    (반복 체이닝)            → 재시도/알림 정책
```

- **빌링키 발급은 프론트/Admin에서 SDK로 처리**한다 (API 방식 대비 카드정보를 백엔드가 직접 다루지 않아 PCI-DSS 부담이 적음)
- **정기결제는 포트원이 서버에서 자동 실행**한다 — 결제 시각(`timeToPay`)을 예약해두면 그 시각에 포트원이 결제를 시도하고, 우리 백엔드는 결과를 웹훅으로만 수신. 반복결제는 매 결제 완료 웹훅을 받을 때마다 "다음 결제"를 다시 예약하는 체이닝으로 구현 (별도 크론/스케줄러 불필요)
- **웹훅**(`POST /webhooks/portone`)은 PortOne JVM Server SDK로 Standard Webhooks 서명을 검증한 뒤 처리. 주요 이벤트: `BillingKey.Issued/Failed`, `Transaction.Paid/Failed/Cancelled`. 포트원이 실패 시 최대 5회 지수 백오프로 재전송하므로, 웹훅 핸들러는 `payment_id` 기준으로 **멱등하게** 처리한다 (중복 수신 대비)
- **Frontend 사용자 API** (`/api/subscriptions/*`, Phase 18 후속 — 최초 구현 시 Admin 전용 조회만 있고 사용자 앱에는 아무 화면이 없던 것을 뒤늦게 보완):
  - `POST /api/subscriptions` — 빌링키로 구독 시작(§ 상단 플로우)
  - `GET /api/subscriptions/me` — 내 구독 상태/다음 결제일 조회. 한 번도 구독한 적 없으면 400
  - `DELETE /api/subscriptions/me` — 구독 해지. **PortOne에 이미 등록된 다음 달 결제 예약(paymentSchedule)을 `revokePaymentSchedules`로 먼저 취소**한 뒤 DB를 CANCELED로 반영한다 — 이 순서가 아니면 DB만 취소되고 포트원은 예정대로 결제를 진행해버린다. 그래서 `subscriptions.next_payment_schedule_id`(V7 마이그레이션)에 마지막으로 등록한 예약 ID를 항상 저장해둔다(최초 구독 시점과, 웹훅의 반복 체이닝 재예약 시점 둘 다). 해지는 즉시 발효되며 남은 기간 일할 환불은 없다(이번 phase 스코프 밖)
  - `GET /api/subscriptions/me/payments` — 내 결제 내역(해지 후 재구독으로 여러 구독 이력이 있어도 전체 통합 조회)
  - `GET /api/usage-quota/me` — 이번 달 AI 생성 사용량/한도 조회(429를 맞기 전에 잔여 횟수를 미리 확인). `ai` 도메인 소속이라 별도 `UsageQuotaController`
- **Frontend 빌링키 발급**은 `@portone/browser-sdk`(`requestIssueBillingKey`, v2)로 처리하며, `storeId`/`channelKey`는 브라우저 노출을 전제로 한 공개 식별자라 `EXPO_PUBLIC_PORTONE_STORE_ID`/`EXPO_PUBLIC_PORTONE_CHANNEL_KEY`로 둔다(API 시크릿/웹훅 시크릿과 달리 비밀값이 아님). 이 SDK는 브라우저 전용이라 **웹 빌드에서만 지원**하고(§4-5 Google 로그인과 동일한 이유 — 동적 import로 감싸 네이티브 번들에 영향 없게 함), 네이티브 지원이 필요해지면 공식 `@portone/react-native-sdk`(Expo config plugin 제공) 도입을 검토한다
- 결제수단은 신용카드/퀵계좌이체로 한정 (간편결제·휴대폰소액결제 등은 PG사와 별도 계약 필요 — 초기 스코프 제외)
- **가격/프로모션 관리(Phase 18 후속)**: Pro 월 요금은 애초 `application.yml`(`app.portone.pro-monthly-price-krw`) 고정값이었으나, Admin이 재배포 없이 가격을 바꾸고 기간제 프로모션가를 설정할 수 있도록 `subscription_pricing`(§5) 단일 행 테이블로 옮겼다(`free_tier_settings`와 동일한 패턴). **가입 시점 가격을 구독별로 잠그지 않는다** — 신규 구독/기존 구독의 다음 달 갱신(재예약) 모두 청구 시점의 유효가(`SubscriptionPricing#effectivePriceKrw` — 프로모션 기간이면 프로모션가, 아니면 기본가)를 그대로 쓴다. 이 값은 `SubscriptionService#chargeFirstPayment`/`#scheduleNextPayment`, `PortOneWebhookService#rescheduleNextPayment`(반복 체이닝의 재예약 시점) 세 지점 모두에서 매번 다시 조회한다 — 이미 포트원에 등록된 예약 건의 금액은 소급 변경되지 않고, 다음 재예약 시점부터만 새 가격이 반영된다.
  - Admin: `GET/PUT /api/admin/subscriptions/pricing/base-price`, `PUT/DELETE /api/admin/subscriptions/pricing/promotion` — 대시보드의 "Pro 구독 가격 관리" 카드에서 조작
  - Frontend(회원): `GET /api/subscriptions/pricing` — `/subscription` 화면이 혜택 목록(무제한 생성/설계 PDF 다운로드 — 실제 구현된 혜택만 소개, [01] 13번의 "고급 분석"은 미구현이라 제외)과 함께 기본가/프로모션가를 보여주는 상품 소개 UI로 이 값을 쓴다

## 4-8. 쿠폰 & 계정 제재 (Phase 19)

**쿠폰**: 이벤트로 외부에 공개하는 코드를 사용자가 직접 등록하면 Pro를 무료로 체험할 수 있다. 핵심은
"구독과 무관하게 이 시각까지는 Pro"를 보장하는 `users.pro_expires_at`(§5) — 결제 웹훅/사용자 해지가
무조건 plan을 FREE로 되돌리던 기존 로직이 이 값을 무시하지 못하게 막았다(`Member#syncPlanFromSubscriptionEnd`
— proExpiresAt이 아직 미래면 FREE로 내리지 않는다). 지급 규칙은 `Member#extendProUntil(days)` 하나로
통일: FREE였다면 오늘부터 N일, 이미 유효한 proExpiresAt이 있었다면(기존 쿠폰이 남아있거나 이미 Pro라면)
그 시점부터 이어서 N일 연장 — "Free는 신규 지급, Pro는 갱신 기간을 혜택만큼 늘려준다"는 요구사항을
연장 시작점(`max(기존 proExpiresAt, now)`) 하나로 구현했다. 이 메서드는 쿠폰 사용(`CouponService`)과
Admin의 일괄 지급(`MemberAdminService#bulkGrantPro`)이 공유한다. 순수 지급/쿠폰만으로 Pro인(결제 구독이
뒷받침하지 않는) 사용자의 만료 처리는 이 코드베이스의 첫 `@Scheduled` 잡인 `ProGrantExpirationScheduler`
(시간당 1회)가 담당 — 활성 구독(ACTIVE/PAST_DUE)이 있는 사용자는 건드리지 않는다(그쪽은 결제 흐름이
관리). `coupons`/`coupon_redemptions`(§5) 2개 테이블, `UNIQUE(coupon_id, user_id)`로 동일 유저의 중복
사용을 DB 레벨에서 막는다. API: `POST /api/coupons/redeem`(회원), `/api/admin/coupons`(코드 생성/목록/
비활성화), `/api/admin/coupons/redemptions`(사용 현황).

`GET /api/auth/me`(`MemberResponse`)도 `proExpiresAt`을 함께 반환한다 — Frontend가 "결제 구독 없이도
Pro인지"를 판별하려면 `plan`만으로는 부족해(구독 여부와 무관하게 Pro일 수 있음) 이 값이 있어야
"쿠폰/프로모션으로 O월 O일까지 Pro" 같은 안내를 정확히 보여줄 수 있다(Phase 19-C/D에서 구독 화면이
`subscription.status`만 보고 쿠폰-Pro 사용자를 Free로 잘못 표시하던 버그를 고치며 추가).

**계정 제재(BAN)**: `permanent_ban`(영구) / `temp_ban_until`(기간제, nullable) 두 필드로 분리해 관리한다
— sentinel 날짜로 영구를 표현하는 방식 대신 boolean을 쓴 이유는 "영구인지 아닌지"가 코드에서 바로
읽히게 하기 위함. **이미 발급된 JWT(액세스 토큰, 최대 30분 유효)까지 즉시 차단**하는 게 핵심이라(그렇지
않으면 "지금 악용 중인 사용자를 막는다"는 제재의 목적이 흐려짐), 기존에 클레임만 신뢰하고 DB를 전혀
조회하지 않던 `JwtAuthenticationFilter`에 회원 조회 1건을 추가했다 — 이 앱 규모에서 PK 조회 1건은
성능상 무시할 수준이라 판단(문제가 되면 이미 Redis를 쓰는 `RefreshTokenStore`처럼 캐시로 옮길 수 있음,
지금은 과설계로 보고 안 함). 제재된 사용자는 403 + `ACCOUNT_BANNED` 코드로 즉시 거부되며, 로그인 시도
자체도 `AuthService.login`/`oauthLogin`/`refresh`에서 동일하게 막는다(재로그인으로 새 토큰을 받아가는
것도 차단). Admin: `POST /api/admin/users/{userId}/ban`(`{permanent, until}`), `.../unban`.

**Admin의 회원 관리 액션**: 기존엔 조회만 가능했던 `MemberAdminController`에 쓰기 액션을 추가했다.
`POST /api/admin/users/pro-grant`(`{userIds[], days}`, `PlanningVersionService#deleteAll`류의 "전체
조회 → 개수 검증 → 없는 id 있으면 통째로 400 → 각각 적용" 패턴), `POST /api/admin/users/{userId}/downgrade`
(강제 Free 전환 — 활성 구독이 있으면 `SubscriptionService#cancelActiveSubscription`을 재사용해 PortOne
예약도 함께 취소한 뒤, 쿠폰 등으로 남은 보장 기간까지 포함해 무조건 Free로 확정).

---

# 5. 데이터베이스 설계 (Supabase PostgreSQL)

버전 관리·설문 불변성 요구사항([01] 5,6,8,9,11 / [02] §7)을 그대로 테이블로 옮긴다. DB는 Supabase가 관리하는 PostgreSQL을 사용하되, 접근은 기존 계획대로 Spring Data JPA + Querydsl + Flyway로 한다 (Supabase 자체 클라이언트/Auth는 사용하지 않음). 실제 마이그레이션은 `database/migarations/`에 Flyway로 작성한다.

| 테이블                 | 주요 컬럼                                                                                                                            | 비고                                                                                                                                       |
| ---------------------- | ------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `users`                | id, email, name, password_hash, provider, provider_id, role, plan, pro_expires_at, temp_ban_until, permanent_ban, withdrawn_at, created_at | `provider=LOCAL\|GOOGLE\|APPLE\|...`, `role=USER\|ADMIN`. `pro_expires_at`(V9)은 쿠폰/관리자 지급으로 구독과 무관하게 보장된 Pro 만료 시각. `name`(V10)은 옵셔널 표시 이름 — NULL이면 프론트가 이메일로 대체 표시 |
| `workspaces`           | id, user_id(FK), name, status, deleted_at, created_at                                                                                | 소프트 삭제                                                                                                                                |
| `survey_definitions`   | id, survey_key, version, title, schema(jsonb)                                                                                        | `unique(survey_key, version)`, Admin에서 발행                                                                                              |
| `survey_responses`     | id, survey_definition_id(FK), workspace_id(FK), answers(jsonb, 암호화), submitted_at                                                 | **불변**                                                                                                                                   |
| `planning_versions`    | id, workspace_id(FK), survey_response_id(FK), version_no, content(jsonb, 암호화), status, deleted_at                                 | status: GENERATING/COMPLETED/FAILED, 소프트 삭제                                                                                           |
| `analysis_versions`    | id, planning_version_id(FK), version_no, content(jsonb, 암호화), status, deleted_at                                                  | survey_response 없음, 소프트 삭제                                                                                                          |
| `design_versions`      | id, analysis_version_id(FK), survey_response_id(FK), version_no, content(jsonb, 암호화), status, deleted_at                          | 소프트 삭제                                                                                                                                |
| `documents`            | id, source_type(PLANNING/ANALYSIS/DESIGN), source_id, file_url, generated_at                                                         | PDF 산출물 (Supabase Storage 경로)                                                                                                         |
| `ai_generation_jobs`   | id, target_type, target_id, status, error_message, created_at                                                                        | 폴링용                                                                                                                                     |
| `subscriptions`        | id, user_id(FK), plan, status, billing_key(암호화), next_billing_at, next_payment_schedule_id, started_at, expires_at                | `status=ACTIVE\|PAST_DUE\|CANCELED`, [01] 13번 BM. `next_payment_schedule_id`(V7)는 사용자 해지 시 PortOne 예약을 취소하기 위한 값         |
| `payment_history`      | id, subscription_id(FK), payment_id, amount, status, paid_at, created_at                                                             | `payment_id` unique — 웹훅 멱등 처리 키                                                                                                    |
| `usage_quotas`         | id, user_id(FK), period(YYYY-MM), generation_count, limit_count                                                                      | 월별 AI 생성 횟수 제한(Phase 20부터 FREE/PRO 모두 적용, 이전엔 FREE만)                                                                     |
| `free_tier_settings`   | id, free_monthly_limit, pro_monthly_limit, created_at, updated_at                                                                    | 단일 행(V5, `subscription_pricing`과 동일 패턴). V10에서 기존 `monthly_limit`을 `free_monthly_limit`으로 rename하고 `pro_monthly_limit` 추가 |
| `subscription_pricing` | id, base_price_krw, promo_price_krw, promo_starts_at, promo_ends_at, updated_at                                                      | 단일 행(V8, `free_tier_settings`와 동일 패턴). promo\_\* 세 컬럼은 전부 NULL 또는 전부 채워짐만 허용                                       |
| `coupons`              | id, code(unique), benefit_days, max_redemptions, redemption_count, expires_at, active, created_at                                    | V9. `code`는 관리자가 직접 지정                                                                                                            |
| `coupon_redemptions`   | id, coupon_id(FK), user_id(FK), redeemed_at, granted_until                                                                           | V9. `unique(coupon_id, user_id)` — 동일 유저 중복 사용 방지                                                                                |

**삭제 정책**: `workspaces`/`*_versions`는 `deleted_at` 기반 소프트 삭제를 기본으로 한다 ([01] 6, 9번의 다중 선택 삭제는 소프트 삭제로 처리, 목록/조회 API에서 필터링). 사용자가 완전 삭제(예: 회원 탈퇴에 따른 개인정보 삭제)를 요청하는 경우에 한해 하드 삭제 API를 별도로 제공한다. 상위 버전(기획)이 소프트 삭제되어도 이를 참조하는 하위 버전(분석/설계)은 조회만 가능하고 재생성은 막는다.

**암호화**: `survey_responses.answers`, `*_versions.content`처럼 사용자의 사업 아이디어가 담긴 필드는 애플리케이션 레벨 암호화(AES-256, JPA `AttributeConverter` 또는 Jasypt)를 적용해 저장한다. 검색/통계가 필요 없는 필드이므로 암호화로 인한 쿼리 제약은 없음.

**RLS(Row Level Security)**: Supabase는 테이블마다 PostgREST 기반 Data API를 자동 노출하며, RLS가 꺼진 테이블은 `anon`/`authenticated` 키만으로 누구나 HTTP로 직접 읽고 쓸 수 있다. 이 프로젝트는 Supabase Auth/클라이언트를 쓰지 않고 backend가 Session Pooler로 테이블 소유자 권한 JDBC 연결만 사용하므로, 모든 도메인 테이블에 RLS를 켜되 정책은 하나도 두지 않는다 — PostgreSQL은 기본적으로 테이블 소유자에게 RLS를 적용하지 않아(`FORCE ROW LEVEL SECURITY` 미설정) backend 접근은 영향이 없고, Data API(소유자 아님)만 완전히 차단된다.

---

# 6. 배포 아키텍처

전 구간 무료 티어로 구성한다.

| 대상                | 플랫폼                           | 비고                                                                                                                                        |
| ------------------- | -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| Admin               | **Vercel**                       | Vite 정적 빌드 배포, 무료 티어                                                                                                              |
| Backend             | **Render**                       | Docker 컨테이너 배포, 무료 Web Service(0.1 CPU/512MB) — 프로젝트 내부 사정으로 Koyeb에서 전환                                               |
| Frontend (네이티브) | **EAS Build (내부/테스트 배포)** | 스토어 정식 출시 아님 — Android는 APK 내부 배포, iOS는 Ad-hoc/TestFlight 수준으로 한정                                                      |
| Frontend (웹)       | **Vercel**                       | `npx expo export -p web` 정적 빌드(`frontend/vercel.json`), Admin과 동일한 무료 티어 배포. Google 로그인은 이 웹 빌드에서만 지원(§4-5 참고) |
| Database/Storage    | Supabase                         | §1/§5에서 이미 확정                                                                                                                         |

**배포 방식**: Render는 GitHub App으로 레포를 직접 watch하다가 push 시 자체적으로 빌드/배포한다 — Koyeb처럼 GitHub Actions에서 API를 호출하는 방식이 아니라, 레포 루트의 `render.yaml`(Blueprint)을 대시보드에서 한 번 연결해두면 별도 워크플로우 없이 자동 배포된다. 모노레포이므로 `buildFilter`로 `backend/**`, `database/migarations/**` 변경 시에만 배포되도록 제한한다. Vercel(Admin/Frontend 웹)도 프로젝트별 Root Directory 설정으로 동일하게 스코프된다.

Frontend 네이티브 빌드(EAS)도 같은 문제가 있다 — Expo 대시보드에서 GitHub App을 연결하면 push마다 자동으로 EAS Build가 도는데, 아무 필터가 없으면 admin/backend/docs만 고친 커밋에도 불필요하게 네이티브 빌드가 돈다. EAS Workflows(`.eas/workflows/build-on-push.yml`)의 `on.push.paths: ["frontend/**"]`로 Render의 `buildFilter`와 동일하게 스코프해둔다 — `main` 브랜치 push + `frontend/**` 변경 시에만 Android/iOS preview(내부 배포용) 빌드가 트리거된다.

**Render 무료 티어 고려사항**

- 15분 무 트래픽 시 **스핀다운**되며(이전 30분에서 단축됨), 다음 요청에서 콜드 스타트 발생 — Koyeb(1시간)보다 훨씬 자주 잠드므로 체감 콜드 스타트 빈도는 오히려 더 높다
- Render 공식 문서는 "약 1분" 내 재기동을 안내하지만, 이는 가벼운 런타임 기준으로 보인다. **Koyeb에서 동일 스펙(0.1 vCPU/512MB)으로 로컬 실측한 결과 Spring Boot 콜드 스타트가 약 7분(427초)까지 걸렸다** — Render free tier도 CPU/RAM 스펙이 완전히 동일(0.1 CPU/512MB)하므로 같은 문제가 재현될 가능성이 높다. **Render 실배포 후 반드시 재실측 필요**
- PortOne 웹훅은 실패 시 최대 5회 지수 백오프로 재전송하지만, 콜드 스타트가 수 분 단위로 걸리면 재시도 윈도우 내 응답 실패 가능성이 있음 — 실측 후 필요 시 keep-alive(주기적 헬스체크 핑)나 유료 플랜 전환 검토
- 0.1 CPU / 512MB 스펙은 Spring Boot 구동에 여유가 크지 않으므로 JVM 힙 튜닝(`-Xmx288m` 등, 로컬 검증 완료)이 필요하며, 콜드 스타트 개선이 더 필요하면 GraalVM Native Image 전환을 검토한다
- 750 free instance hours/월, 100GB 대역폭/월, 500 빌드 분/월 제한 — 트래픽이 늘어나면 유료 플랜으로 전환
