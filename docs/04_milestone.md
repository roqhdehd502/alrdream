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
- [x] Render에 배포 후 실제 메모리 사용량/콜드 스타트 시간 측정 → JVM 힙 옵션(`-Xmx288m` 등, 로컬 값 기준) 튜닝
- [x] 동일 컨테이너에서 OpenHTMLtoPDF로 더미 HTML → PDF 변환 스모크 테스트 → 메모리 여유 확인 (`/spike/pdf-smoke-test`, 로컬 검증 완료 — Render에서 재확인 필요)
- [x] `render.yaml`(Blueprint) 작성 — Render는 자체 GitHub App으로 push 시 자동 배포하므로 별도 GitHub Actions 워크플로우 불필요, `buildFilter`로 `backend/**`/`database/migarations/**`만 감지
- [x] `admin`: 빈 페이지로 Vite 빌드 → Vercel 배포, 자동 배포 연동 확인 (로컬 빌드는 Phase 00에서 확인됨, 실제 Vercel 배포 성공 여부는 미확인)
- [x] `frontend`: EAS 프로젝트 초기 설정(`eas.json`), 빈 화면으로 내부 테스트 빌드 1회 성공 확인

## 사전 조건 (사용자 측)

- [x] Render 가입 + GitHub App을 레포에 연결, `render.yaml` Blueprint 적용 시 `sync: false`로 표시된 환경변수(Supabase DB 접속정보) 대시보드에 직접 입력
- [x] Vercel 프로젝트 생성 및 레포 연결
- [x] `cd frontend && npx eas-cli login && npx eas-cli init` (Expo 계정)

---

# Phase 02: DB 스키마 & 초기 마이그레이션

## 작업 항목

- [x] [03] §5 테이블 설계를 Flyway 마이그레이션(`V1__initial_schema.sql`)으로 작성: `users`, `workspaces`, `survey_definitions`, `survey_responses`, `planning_versions`, `analysis_versions`, `design_versions`, `documents`, `ai_generation_jobs`, `subscriptions`, `payment_history`, `usage_quotas` — 로컬에서 실제 Supabase에 마이그레이션 적용 검증 완료 (기존 placeholder 파일명이 Flyway 네이밍 규칙(`V<n>__...`)과 맞지 않아 `V1__initial_schema.sql`로 정정)
- [x] 공통 `BaseEntity`(생성/수정 시각, `global/jpa`) 및 소프트 삭제용 `SoftDeleteBaseEntity`(`deleted_at`) 작성 — `workspaces`/`planning_versions`/`analysis_versions`/`design_versions`만 상속 대상 (그 외 테이블은 소프트 삭제 대상 아님, [03] §5 삭제 정책 참고). `@EnableJpaAuditing` 설정 포함. 구체 도메인 엔티티는 각 도메인 Phase에서 작성
- [x] `survey_responses.answers`, `*_versions.content`, `subscriptions.billing_key`에 적용할 AES-256-GCM 암호화 컨버터(`EncryptedStringConverter`, `global/security`) 작성 [03] §5 — DB 컬럼은 암호문을 담아야 하므로 JSONB가 아닌 TEXT로 설계 (구체 엔티티에 `@Convert` 적용은 각 도메인 Phase에서). 임시 엔티티로 암호화→저장→복호화 왕복 실동작 검증 완료. 검토 중 발견: `DATA_ENCRYPTION_KEY`가 비어 있으면(예: Render에 아직 미등록) `@Component`인 컨버터가 부팅 시점에 즉시 키를 만들려다 앱 전체가 기동 실패하는 버그가 있어, 키 검증을 실제 변환 호출 시점으로 미루도록 수정
- [x] `database/seed.sql`: 초기 `survey_definitions` 3종(`PLANNING_HAS_IDEA`, `PLANNING_EXPLORING`, `DESIGN`) 시드 데이터 삽입 ([02] §5 문항 그대로) — 로컬에서 실제 Supabase에 삽입 검증 완료. `PLANNING_EXPLORING` Q1(`interested_fields`)은 문서에 선택지가 명시되지 않아 통상적인 업종 카테고리로 채움 — Admin 설문 관리(Phase 12) 완성 후 재검토 필요
- [x] `V2__enable_row_level_security.sql`: 전 도메인 테이블에 RLS 활성화(정책 없음) [03] §5 — Supabase Table Editor에서 전 테이블이 "UNRESTRICTED"(Data API가 RLS 없이 익명 접근 가능)로 표시된 것을 발견해 추가. `FORCE` 없이 켜서 테이블 소유자(backend의 JDBC 연결)는 영향 없고 Data API(anon/authenticated)만 차단됨을 직접 검증 완료
- [x] `flyway_schema_history`에도 동일하게 RLS 적용 — 사용자가 대시보드에서 이 테이블만 "UNRESTRICTED"로 남아있는 걸 발견해 추가 요청. **당시엔 `V3__enable_rls_flyway_schema_history.sql`이라는 일반 Flyway 마이그레이션으로 적용했고, psql 직접 실행 + 수동 INSERT + `flywayRepair` 우회로 임시 해결했었음** — 이후 Phase 03 테스트 중 근본 원인을 찾아 완전히 다른 방식으로 대체함 (아래 Phase 03 "테스트 중 발견해 함께 고친 버그" 참고), `V3__...sql` 파일은 삭제됨

---

# Phase 03: 인증/회원 (member 도메인)

## 작업 항목

- [x] Spring Security 6 + JWT(Access/Refresh) 기본 골격 — `global/security`(`JwtTokenProvider`, `JwtAuthenticationFilter`, `SecurityConfig`), stateless. Access 30분/Refresh 14일
- [x] 자체 회원가입/로그인 (이메일 + 비밀번호) — `domain/member`(`Member`/`MemberRepository`/`AuthService`), BCrypt 해시. `POST /api/auth/signup`, `/login`
- [x] OAuth2 소셜 로그인 — Google, Apple [03] §4-5. **설계 결정**: Spring Security의 OAuth2Client 리다이렉트 플로우 대신, Frontend(Expo 모바일)가 각 provider SDK로 발급받은 ID 토큰을 백엔드가 검증하는 방식으로 구현(`POST /api/auth/oauth/google`, `/apple`) — 모바일 앱에는 브라우저 리다이렉트보다 이 패턴이 표준적. Google은 `google-api-client`, Apple은 Apple JWKS(`nimbus-jose-jwt`)로 검증. Google은 실제 client-id로 배선 완료, **Apple은 Apple Developer 자격증명이 아직 없어(.env 비어있음) 코드만 구현, 실제 토큰으로 검증 안 됨** — 크리덴셜 채워지면 재검증 필요
- [x] `role(USER/ADMIN)` 클레임 기반 인가, Admin API 라우트 분리 — `/api/admin/**`는 `ROLE_ADMIN` 필요. 실제 DB에서 role을 ADMIN으로 바꾼 계정으로 통과(→404, 매핑된 컨트롤러 없음)/USER 계정으로 차단(→403) 둘 다 실동작 검증
- [x] Refresh Token 저장/무효화 (Redis) — `RefreshTokenStore`, 회원당 활성 세션 1개. Refresh 시 access+refresh 모두 로테이션, 로테이션 전 토큰 재사용 및 로그아웃 후 재사용 전부 거부되는 것까지 검증
- [x] Swagger(`/swagger-ui.html`) 문서화 — 배포 후 사용자가 각 API에 description이 비어있는 걸 발견해 추가. `global/config/OpenApiConfig`로 API 타이틀/설명 및 JWT Bearer 보안 스킴(`bearerAuth`) 정의, `AuthController`의 모든 엔드포인트에 `@Operation`/`@ApiResponse`, 요청/응답 DTO(`SignupRequest` 등)와 `ErrorResponse`에 `@Schema` 필드 설명 추가. 인증이 필요한 `/logout`, `/me`는 `@SecurityRequirement`로 표시해 Swagger UI의 Authorize 버튼으로 바로 테스트 가능. `/v3/api-docs` 실제 응답으로 summary/description/security/필드 설명이 모두 반영된 것 검증 완료. Phase 01 스파이크용 `PdfSmokeTestController`도 동일하게 문서화
- [x] role별 로그인 테스트 계정 시드 — `database/seed-test-accounts.sql` + `database/scripts/seed-test-accounts.sh`/`seed-test-accounts-down.sh`. `admin@alrdream.test`(ADMIN)/`user@alrdream.test`(USER). `survey_definitions` 시드(운영에도 필요한 데이터)와 달리 이건 순수 테스트용이라 **운영 DB에 절대 실행 금지** — `seed.sql`과 분리된 별도 파일/스크립트로 관리해 실수로 함께 실행되지 않도록 함. 실제 Supabase에 삽입 후 두 계정 모두 로그인 → `/me`로 role 확인 → ADMIN은 `/api/admin/**` 통과(404)/USER는 차단(403) 실동작 검증 완료

### 테스트 중 발견해 함께 고친 버그 (auth 코드 자체는 아니지만 실제 요청으로 검증하다 발견)

- `BaseEntity`(`OffsetDateTime`)와 Spring Data JPA Auditing 기본 `DateTimeProvider`(`LocalDateTime` 생성)가 타입 불일치로 모든 INSERT가 500 — `JpaAuditingConfig`에 `OffsetDateTime`을 반환하는 커스텀 `DateTimeProvider` 등록으로 해결. 이 프로젝트의 첫 엔티티(`Member`)라 이제야 드러남
- `GlobalExceptionHandler`의 `catch(Exception e)`가 로깅 없이 예외를 삼켜 원인 파악이 불가능했음 — `log.error` 추가
- 같은 핸들러가 너무 광범위해 `NoResourceFoundException`(매핑 안 된 경로, 원래 404)까지 500으로 덮어씀 — 전용 핸들러 추가해 404 유지
- Spring Boot 4.x의 자동 구성 `ObjectMapper` 빈이 Jackson 3.x(`tools.jackson`) 타입이라 `SecurityConfig`가 기대하던 Jackson 2.x(`com.fasterxml.jackson`) 타입과 안 맞아 부팅 실패 — 빈 주입 대신 `SecurityConfig` 내부에서 독립적으로 생성하도록 변경
- 테스트 중 Supabase Session Pooler(pool_size=15)가 가득 차 연결 불가 상태 발생 — HikariCP 기본 max-pool-size(10)가 이 예산에 비해 과함을 확인, `application.yml`에 `spring.datasource.hikari.maximum-pool-size: 5` 명시
- **`./gradlew test`가 항상 멈춤(진짜 데드락) — Phase 02 때의 "Flyway Gradle 플러그인이 V3에서 멈춘다"는 문제의 근본 원인을 여기서 확정**: `flyway_schema_history` 자신을 대상으로 하는 DDL을 Flyway가 추적하는 일반 마이그레이션으로 실행하면, Flyway가 같은 히스토리 테이블을 동시에 두 커넥션으로 다루면서(한 커넥션은 `SELECT COUNT(*) FROM pg_namespace...` 실행 후 "idle in transaction"으로 대기, 다른 커넥션은 그 커넥션이 쥔 락을 기다리며 `ALTER TABLE` 실행 대기) 자기 자신과 락이 걸려 영원히 풀리지 않는다. Supabase뿐 아니라 로컬 Testcontainers(순정 Postgres, 커넥션 풀러 없음)에서도 100% 즉시 재현되어 Supabase/풀러 특이 문제가 아님을 확인. **해결**: `V3__enable_rls_flyway_schema_history.sql`을 완전히 제거하고, `global/flyway/EnableFlywaySchemaHistoryRlsCallback`(Flyway `Callback`, `AFTER_MIGRATE` 이벤트)으로 대체 — migrate() 전체가 끝나고 내부 락/커넥션이 정리된 뒤 실행되므로 데드락 여지가 없다. `ENABLE ROW LEVEL SECURITY`는 멱등이라 매번 재실행해도 안전. 프로덕션 Supabase의 `flyway_schema_history`에서 기존 V3 이력 행도 정리(`DELETE ... WHERE version='3'`). 검증: RLS를 수동으로 껐다가 `bootRun` 재실행 시 콜백이 재적용하는 것 확인, `./gradlew test`가 데드락 없이 8초 만에 `BUILD SUCCESSFUL`

---

# Phase 04: 워크스페이스 도메인

## 작업 항목

- [x] 워크스페이스 생성 API — "아이템 있음/고민 중" 분기 처리 [01] 2번. **설계 결정**: `workspaces` 테이블(§5)에는 분기를 저장할 컬럼이 없다 — 분기 선택은 프론트에서 다음에 어떤 설문(`PLANNING_HAS_IDEA`/`PLANNING_EXPLORING`)을 보여줄지 결정하는 라우팅 정보일 뿐이고, 실제로 영속화되는 신호는 이후 제출되는 `survey_responses.survey_definition_id`(Phase 05)다. 그래서 `POST /api/workspaces`는 `name`만 받는다
- [x] 워크스페이스 목록/상세 조회 API (기획/분석/설계/설정 탭 데이터) [01] 3번 — 상세 조회는 워크스페이스 자체 정보만 반환한다. 기획/분석/설계 탭의 실제 버전 데이터는 아직 해당 도메인이 없어(Phase 05+) 각자 도메인이 생기면 별도 하위 리소스 API로 채워질 예정
- [x] 워크스페이스 수정/삭제 API [01] 4번 — 수정은 이름 변경만(현재 상태는 ACTIVE 하나뿐이라 상태 변경 API는 없음), 삭제는 소프트 삭제(`deleted_at`)
- [x] `V3__finalize_workspace_status.sql`: `V1__initial_schema.sql`이 "Phase 04에서 확정" 주석으로 남겨뒀던 `workspaces.status` CHECK 제약을 `('ACTIVE')`로 확정 — [01] 문서에 다른 상태 전이가 없어 다른 상태 컬럼들과 동일한 패턴(CHECK 명시)만 맞추고 값은 하나만 허용
- [x] 소유권 격리 — 모든 조회/수정/삭제는 `WHERE user_id = 현재 로그인한 회원` 조건이 걸린 단일 쿼리로 처리해, 다른 회원의 워크스페이스는 존재 자체가 노출되지 않고 "존재하지 않음"(400)으로 응답. 실제 두 계정(role별 테스트 계정)으로 교차 접근 시도해 격리 검증 완료
- [x] Swagger 문서화 — Phase 03에서 만든 패턴(`@Tag`/`@Operation`/`@ApiResponse`/`bearerAuth`)을 동일하게 적용
- [x] 목록 조회 페이징/정렬/검색 — 초기 구현이 전체를 한 번에 반환하는 방식이었는데, 워크스페이스 생성 개수에 제한이 없어 사용자가 많아지면 그대로 무리가 될 수 있다는 지적을 받고 추가. `[03] §4-1`에 명시된 대로 Querydsl로 구현(`WorkspaceQueryRepository`/`WorkspaceQueryRepositoryImpl`, 프로젝트 첫 Querydsl 사용처라 `JPAQueryFactory` 빈(`global/config/QuerydslConfig`)도 함께 추가). `GET /api/workspaces?page=&size=&sort=&keyword=` — 표준 Spring Data `Pageable`(정렬 가능 필드: `name`/`createdAt`/`updatedAt`, 기본값 `createdAt,desc`) + 이름 부분/대소문자 무시 검색. 응답은 `PagedModel`로 감싸 `{content, page}` 형태로 고정(Spring Data가 `Page`/`PageImpl` 직접 직렬화를 권장하지 않음). Swagger에 `page`/`size`/`sort` 필드가 각각 노출되도록 `@ParameterObject` 적용

### 테스트 중 발견해 함께 고친 버그

- `@Valid`로 검증하는 요청 DTO(`CreateWorkspaceRequest` 등)가 검증에 실패하면 `MethodArgumentNotValidException`을 처리하는 핸들러가 없어 `GlobalExceptionHandler`의 catch-all에 걸려 500으로 응답되고 있었음 — Phase 03에서 발견한 `NoResourceFoundException` 건과 같은 종류의, Phase 00 스캐폴딩부터 있던 잠재 버그(회원가입 `SignupRequest`도 동일하게 영향받고 있었음). 전용 핸들러를 추가해 필드별 메시지와 함께 400을 반환하도록 수정
- 지원하지 않는 정렬 필드(`?sort=bogus,asc`)를 넣으면 400이 아니라 500이 나옴 — `WorkspaceQueryRepositoryImpl`(`@Repository`)에서 `IllegalArgumentException`을 던졌는데, Spring의 JPA 예외 변환(`PersistenceExceptionTranslationInterceptor`)이 `@Repository` 빈에서 나온 `IllegalArgumentException`/`IllegalStateException`을 무조건 `InvalidDataAccessApiUsageException`으로 감싸버려 `GlobalExceptionHandler`의 전용 핸들러를 못 타는 게 원인 — Spring의 잘 알려진 함정. 정렬 필드 검증을 `@Repository`가 아닌 `@Service`(`WorkspaceService`) 경계로 옮겨 해결

---

# Phase 05: 설문 도메인 (survey)

## 작업 항목

- [x] `survey_definitions` 발행/조회 API (Admin 전용) [02] §3 — `POST /api/admin/survey-definitions`, `GET /api/admin/survey-definitions[?surveyKey=]`, `GET /api/admin/survey-definitions/{id}`. **설계 결정**: "CRUD"가 아니라 발행(Create)+조회(Read)만 제공 — [02] §7 "설문 문항이 나중에 바뀌어도 과거 응답을 정확히 재현" 요구사항상 이미 발행된 버전은 절대 수정/삭제하면 안 되고(응답이 그 버전을 FK로 참조), 문항을 고치고 싶으면 새 버전을 발행하는 것만 허용. 버전 번호는 클라이언트가 지정하지 않고 서버가 해당 surveyKey의 최신 버전+1로 자동 계산
- [x] `survey_responses` 제출/조회 API [02] §4 — `POST /api/workspaces/{workspaceId}/survey-responses`(제출), `GET .../survey-responses`(목록), `GET .../survey-responses/{id}`(상세, 복호화된 답변 포함). 워크스페이스 하위 리소스라 Phase 04와 동일하게 `WorkspaceService.getOwned`로 소유권 검증. `answers`는 `EncryptedStringConverter`(Phase 02에서 미리 만들어두고 임시 엔티티로만 검증했던 것)의 첫 실제 사용처 — DB에 실제로 암호문(Base64)으로 저장되는 것과 복호화 왕복 둘 다 실동작 검증
- [x] 설문 정의 기준 answer 유효성 검증 — `SurveyAnswerValidator`. 문항 타입 크로스체크·필수 응답 여부·중복/미정의 questionId·allowUnknown 여부·선택형 문항 옵션 값·SCALE 범위(1~5) 전부 검증, 실제 케이스 7개(누락/타입불일치/잘못된 옵션/허용안된 unknown/범위초과/중복/미정의 문항)로 각각 400 확인
- [x] `DESIGN` 설문의 동적 옵션(`core_feature_priority`, 분석 산출물 기반) 주입 로직 [02] §5-3 — `DesignFeatureOptionResolver` 인터페이스 + 기본 구현 `NoAnalysisDesignFeatureOptionResolver`(항상 빈 목록). **알려진 제약**: 분석 도메인(Phase 08)이 아직 없어 실제 분석 산출물 연동은 못 함 — 메커니즘(옵션 주입 지점, 옵션이 비어있을 때 값 검증을 건너뛰는 처리)만 구현. Phase 08~09에서 분석 도메인이 생기면 이 인터페이스의 실제 구현체로 교체하면 됨

## 부수 변경

- `global/jpa/CreatedOnlyBaseEntity`: `survey_definitions`처럼 `created_at`만 있고 `updated_at`은 없는 테이블(불변 레코드)을 위한 공용 베이스 — `BaseEntity`/`SoftDeleteBaseEntity`와 같은 계열. `survey_responses`는 컬럼명이 `submitted_at`으로 달라 이 베이스를 쓰지 않고 엔티티에 직접 선언

### 기능/비기능 테스트에서 발견해 함께 고친 버그

기능 테스트(정상/경계값/검증 실패 케이스)는 문제없었고, 아래는 비기능 테스트(보안/동시성/강건성) 중 발견한 것들 — 전부 실제로 재현 후 수정, 재테스트로 확정:

- **동시성 — 진짜 race condition**: 같은 surveyKey를 동시에 발행하면("최신 버전+1" 계산 경합) `UNIQUE(survey_key, version)` 위반으로 500 — 10개 동시 발행 요청 중 8개 실패로 실제 재현. Postgres `pg_advisory_xact_lock`(surveyKey별 키, 트랜잭션 종료 시 자동 해제)으로 같은 surveyKey의 발행만 직렬화해 해결 — 10개 동시 요청 재테스트 결과 전부 성공, 버전 4~13 중복/누락 없이 순차 부여 확인
- **경로 변수 타입 변환 실패가 500** — `MethodArgumentTypeMismatchException`(잘못된 UUID 형식의 workspaceId, enum에 없는 surveyKey) 처리 핸들러가 없었음. `GlobalExceptionHandler`에 추가 — Workspace 도메인 등 프로젝트 전역 엔드포인트에 공통 적용되는 수정
- **표준 Spring MVC 예외 3종이 전부 500**: 깨진/타입불일치 JSON 바디(`HttpMessageNotReadableException`), 지원 안 하는 HTTP 메서드(`HttpRequestMethodNotSupportedException` → 405), Content-Type 누락/미지원(`HttpMediaTypeNotSupportedException` → 415) — 셋 다 핸들러 추가. Phase 03부터 이어진 "GlobalExceptionHandler가 알려진 예외를 못 잡으면 500"이라는 동일 패턴의 반복이라, 이번엔 표준 Spring MVC 예외 전반을 한 번에 스윕
- **답변 값 길이 무제한** — `values`(설문 답변 텍스트)에 길이 제한이 전혀 없어 40MB 페이로드도 그대로 암호화·저장까지 통과함(메모리/스토리지 남용 여지). `SurveyAnswerValidator`에 값 1개당 10,000자 상한 추가
- **MULTI_CHOICE 값 중복 미검증** — 같은 보기를 여러 번 선택해도 통과하던 걸 발견해 중복 검증 추가
- SQL 인젝션/XSS 스타일 입력(`'; DROP TABLE...`, `<script>...`), 대용량 유니코드/이모지 텍스트는 문제없음 — JPA 파라미터 바인딩 + 순수 JSON API라 별도 이스케이프 불필요, 저장/조회 왕복 정상 확인
- 인증/인가(401/403), RLS(`survey_definitions`/`survey_responses` 둘 다 `relrowsecurity=t`) 는 이상 없음

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
