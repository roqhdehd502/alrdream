# 마일스톤

[01] 기획및분석, [02] AI 설문, [03] 설계 문서를 기준으로 Phase를 나눈다. 가장 큰 인프라 리스크(Render 무료 티어에서 Spring Boot + PDF 렌더링이 실제로 동작하는지)를 Phase 01에서 먼저 검증한 뒤, 백엔드 도메인은 의존 순서(DB → 인증 → 워크스페이스 → 설문 → AI 연동 → 기획/분석/설계 → PDF → 구독)를 따른다. Admin/Frontend는 관련 백엔드 API가 준비된 시점부터 시작한다.

---

# Phase 00: 프로젝트 스캐폴딩 & 로컬 개발 환경

> `admin/`, `backend/`, `frontend/`, `database/` 디렉토리 골격은 이미 존재. 각 앱의 실제 프로젝트 초기화가 필요.

## 작업 항목

- [x] `backend/`: Spring Boot 4.x + Gradle 프로젝트 초기화 (Java 21) — start.spring.io가 3.x 생성을 중단(`compatibility range >=4.0.0`)해 4.x로 시작, [03] §4-1 패키지 구조(`global`/`domain`/`infrastructure`)로 세팅
- [x] `frontend/`: Expo 프로젝트 초기화 (Expo Router, react-native-web) [03] §3-2
- [x] `admin/`: Vite + React + TypeScript 프로젝트 초기화 [03] §2-2
- [x] `database/migarations/`를 backend의 Flyway 마이그레이션 경로로 연결
- [x] 로컬 개발용 `docker-compose.yml` 작성 (Redis 등 — DB/Storage는 Supabase 직접 연결이라 로컬 컨테이너 불필요)
- [x] `.env` / `application.yml` 템플릿 정리 (Supabase 접속정보, Claude API Key, OAuth Client ID/Secret 등 시크릿 placeholder만 커밋)

## 사전 조건 (사용자 측)

- [x] Supabase 프로젝트 생성 (PostgreSQL + Storage)
- [x] Anthropic Claude API 키 발급
- [x] Google OAuth Client 등록
- [x] Apple Developer 계정 및 Sign in with Apple 설정
- [x] 포트원(PortOne) 가맹점 가입 + 토스페이먼츠(신모듈) PG 채널 연동, 웹훅 시크릿 발급 [03] §4-7

---

# Phase 01: 배포 파이프라인 & 기술 스파이크

> Render 무료 Web Service(0.1 CPU/512MB)가 Spring Boot + OpenHTMLtoPDF를 실제로 감당하는지, EAS/Vercel 배포 설정이 문제없이 동작하는지를 본 개발 전에 먼저 검증한다. 여기서 막히면 [03] §6의 플랫폼 선택 자체를 재검토해야 하므로 최대한 앞단에 배치한다.
>
> (2026-08-03: 배포 대상을 Koyeb → Render로 변경. 로컬에서 동일 스펙(0.1 vCPU/512MB)으로 실측한 결과 Spring Boot 콜드 스타트가 약 7분 걸렸음 — Render도 스펙이 동일해 재현 가능성 높음, Render 실배포 후 재검증 필요)

## 작업 항목

- [x] `backend`: 최소 Hello World 컨트롤러 + Supabase Postgres 연결 확인용 헬스체크 엔드포인트(`/actuator/health`) 포함 Dockerfile 작성 — 로컬 검증 완료(무제한 CPU 5.3s / 0.1 vCPU 시뮬레이션 427s, 메모리는 512MB 중 약 277MB)
- [ ] Render에 배포 후 실제 메모리 사용량/콜드 스타트 시간 측정 → JVM 힙 옵션(`-Xmx288m` 등, 로컬 값 기준) 튜닝
- [x] 동일 컨테이너에서 OpenHTMLtoPDF로 더미 HTML → PDF 변환 스모크 테스트 → 메모리 여유 확인 (`/spike/pdf-smoke-test`, 로컬 검증 완료 — Render에서 재확인 필요)
- [ ] `render.yaml`(Blueprint) 작성 — Render는 자체 GitHub App으로 push 시 자동 배포하므로 별도 GitHub Actions 워크플로우 불필요, `buildFilter`로 `backend/**`/`database/migarations/**`만 감지
- [ ] `admin`: 빈 페이지로 Vite 빌드 → Vercel 배포, 자동 배포 연동 확인 (로컬 빌드는 Phase 00에서 확인됨, 실제 Vercel 배포 성공 여부는 미확인)
- [ ] `frontend`: EAS 프로젝트 초기 설정(`eas.json`), 빈 화면으로 내부 테스트 빌드 1회 성공 확인

## 사전 조건 (사용자 측)

- [ ] Render 가입 + GitHub App을 레포에 연결, `render.yaml` Blueprint 적용 시 `sync: false`로 표시된 환경변수(Supabase DB 접속정보) 대시보드에 직접 입력
- [x] Vercel 프로젝트 생성 및 레포 연결
- [ ] `cd frontend && npx eas-cli login && npx eas-cli init` (Expo 계정)

---

# Phase 02: DB 스키마 & 초기 마이그레이션

## 작업 항목

- [ ] [03] §5 테이블 설계를 Flyway 마이그레이션(`V1__init.sql`)으로 작성: `users`, `workspaces`, `survey_definitions`, `survey_responses`, `planning_versions`, `analysis_versions`, `design_versions`, `documents`, `ai_generation_jobs`, `subscriptions`, `payment_history`, `usage_quotas`
- [ ] 공통 `BaseEntity`(생성/수정 시각) 및 소프트 삭제용 `deleted_at` 적용 대상 테이블 정리
- [ ] `survey_responses.answers`, `*_versions.content`에 대한 암호화 컨버터(`AttributeConverter`) 적용 [03] §5
- [ ] `database/seed.sql`: 초기 `survey_definitions` 3종(`PLANNING_HAS_IDEA`, `PLANNING_EXPLORING`, `DESIGN`) 시드 데이터 삽입 ([02] §5 문항 그대로)

---

# Phase 03: 인증/회원 (member 도메인)

## 작업 항목

- [ ] Spring Security 6 + JWT(Access/Refresh) 기본 골격
- [ ] 자체 회원가입/로그인 (이메일 + 비밀번호)
- [ ] OAuth2 소셜 로그인 — Google, Apple [03] §4-5
- [ ] `role(USER/ADMIN)` 클레임 기반 인가, Admin API 라우트 분리
- [ ] Refresh Token 저장/무효화 (Redis)

---

# Phase 04: 워크스페이스 도메인

## 작업 항목

- [ ] 워크스페이스 생성 API — "아이템 있음/고민 중" 분기 처리 [01] 2번
- [ ] 워크스페이스 목록/상세 조회 API (기획/분석/설계/설정 탭 데이터) [01] 3번
- [ ] 워크스페이스 수정/삭제 API [01] 4번

---

# Phase 05: 설문 도메인 (survey)

## 작업 항목

- [ ] `survey_definitions` CRUD API (Admin 전용, 버전 발행) [02] §3
- [ ] `survey_responses` 제출/조회 API [02] §4
- [ ] 설문 정의 기준 answer 유효성 검증 (문항 타입·필수 여부·`allowUnknown` 매칭)
- [ ] `DESIGN` 설문의 동적 옵션(`core_feature_priority`, 분석 산출물 기반) 주입 로직 [02] §5-3

---

# Phase 06: AI 연동 인프라

## 작업 항목

- [ ] `AiClient` 인터페이스 + Claude API 구현체 (`@HttpExchange`) [03] §4-3
- [ ] `PromptBuilder` — `promptKey` → 프롬프트 템플릿 변수 매핑 [02] §6
- [ ] Claude Tool Use 기반 Structured Output 파싱 (섹션별 JSON 스키마 강제)
- [ ] `ai_generation_jobs` 비동기 처리 (Virtual Thread) + Job 상태 폴링 API [03] §4-4
- [ ] `ai_generation_jobs` 생성 전 Free 티어 사용량(`usage_quotas`) 체크 — 초과 시 429 응답 [01] 13번 (생성 진입점이 이 한 곳뿐이라 여기서 공통 처리)

---

# Phase 07: 기획(Planning) 도메인

## 작업 항목

- [ ] 기획 생성 — 설문 응답 기반 AI 생성 [01] 2번
- [ ] 기획 수정 — 이전 응답 불러와 편집 후 재생성, 버전 기록 [01] 5번
- [ ] 기획 다중 삭제 (소프트 삭제) [01] 6번

---

# Phase 08: 분석(Analysis) 도메인

## 작업 항목

- [ ] 분석 생성 — 설문 없이 기획 본문을 입력으로 AI 생성, 합법여부/가용 리소스/경쟁 서비스 포함 [01] 7번
- [ ] 분석 수정 (버전 기록) [01] 8번
- [ ] 분석 다중 삭제 (소프트 삭제) [01] 9번

---

# Phase 09: 설계(Design) 도메인

## 작업 항목

- [ ] 설계 생성 — `DESIGN` 설문 + 분석 결과를 함께 입력으로 AI 생성 [01] 10번
- [ ] 설계 수정 — 이전 응답 불러와 편집 후 재생성, 버전 기록 [01] 11번

---

# Phase 10: PDF 생성 파이프라인

> Phase 01에서 검증한 OpenHTMLtoPDF 파이프라인을, 실제 기획/분석/설계 콘텐츠 구조([01] 12-4)에 맞춰 완성한다.

## 작업 항목

- [ ] `content(JSON)` → Thymeleaf 템플릿 렌더링 ([01] 12-4 구조: 아이디어 요약~리스크) [03] §4-6
- [ ] OpenHTMLtoPDF로 HTML → PDF 변환
- [ ] Supabase Storage 업로드 + `documents` 레코드 저장, 서명 URL 발급

---

# Phase 11: 구독/결제 (subscription)

## 작업 항목

- [ ] PortOne 빌링키 발급 연동 — Frontend/Admin SDK(`requestIssueBillingKey`) → 백엔드 `billingKeyId` 수신/암호화 저장 [03] §4-7
- [ ] 최초 결제 요청 + 다음 달 결제 예약(`timeToPay`) API
- [ ] `POST /webhooks/portone` — Standard Webhooks 서명 검증(JVM SDK), `payment_id` 기준 멱등 처리
- [ ] 웹훅 이벤트 처리 — `Transaction.Paid`(결제 이력 저장 + 다음 결제 재예약), `Transaction.Failed`(`status=PAST_DUE` 전환)
- [ ] Pro 구독 권한 반영 — 무제한 생성, 고급 분석, 설계 문서 export

---

# Phase 12: Admin 앱

## 작업 항목

- [ ] 설문 정의 관리 화면 — 목록/에디터/버전 발행/미리보기 [03] §2
- [ ] 사용자/워크스페이스 조회 화면 (CS 대응용)
- [ ] AI 프롬프트 템플릿 관리 화면
- [ ] 구독/사용량 대시보드

---

# Phase 13: Frontend 앱 (사용자)

## 작업 항목

- [ ] 로그인/회원가입 화면 (자체 + Google/Apple)
- [ ] 워크스페이스 목록 및 생성 플로우 (분기 선택 → 설문) [03] §3-1
- [ ] 설문 엔진 — 문항 타입별 공용 컴포넌트 (`SingleChoiceField`/`MultiChoiceField`/`TextField`/`ScaleField`) [03] §3-3
- [ ] AI 생성 대기 화면 (Job 폴링)
- [ ] 워크스페이스 상세 — 기획/분석/설계/설정 탭
- [ ] PDF 열람/다운로드

---

# Phase 14: 프로덕션 릴리즈 마무리

> 기본 배포 파이프라인은 Phase 01에서 이미 구축됨. 여기서는 완성된 전 기능을 실제로 얹고 최종 점검한다.

## 작업 항목

- [ ] 전체 환경변수/시크릿(Supabase, Claude, OAuth, PortOne) Render/Vercel에 최종 등록
- [ ] PortOne 웹훅 URL을 실제 배포 도메인으로 등록, 콜드 스타트 상황에서 웹훅 재전송이 정상 처리되는지 확인
- [ ] `frontend` — EAS Build로 내부 테스트 배포 (Android APK, iOS Ad-hoc/TestFlight) — 스토어 정식 출시는 스코프 아님
- [ ] E2E 스모크 테스트 — 회원가입 → 워크스페이스 생성 → 기획/분석/설계 생성 → PDF 다운로드 → Pro 구독 결제까지 전체 플로우 수동 점검

## 사전 조건 (사용자 측)

- [ ] 도메인 및 SSL 인증서 준비 (선택 — Vercel/Render 기본 서브도메인으로 우선 진행 가능)
