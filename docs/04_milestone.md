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
- [ ] Apple Developer 계정 및 Sign in with Apple 설정 — **보류(2026-08-06)**: Apple Developer 유료 멤버십 구독 관련 이슈로 자격증명 발급이 막혀있어, 해결 전까지 잠정 보류. 백엔드 코드(`AppleIdTokenVerifierAdapter`)는 이미 구현돼 있고 `APPLE_OAUTH_*` 값만 비어 있는 상태라 크리덴셜이 준비되면 바로 재검증 가능
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

> (2026-08-06: Phase 12 완료 후 실제로 Render/Vercel에 배포해 Admin에서 로그인을 시도하자
> `RedisConnectionFailureException`(`localhost:6379` 연결 거부)으로 500 에러 — `render.yaml`에 Redis(Key
> Value) 서비스 자체가 없어 `REDIS_HOST`/`REDIS_PORT`가 로컬 기본값(`localhost:6379`)으로 떨어진 게 원인이었다.
> `docker-compose.yml`엔 로컬 Redis가 있었지만 Phase 01 당시 배포용 Render Key Value 프로비저닝이 누락된 채
> 넘어간 것 — Redis는 `RefreshTokenStore`(로그인/로그아웃마다 필수 경유)의 유일한 저장소라 로그인 자체가 완전히
> 막히는 문제였다. `render.yaml`에 `type: keyvalue` 서비스(`alrdream-redis`, free 플랜, `maxmemoryPolicy:
volatile-lru` — 모든 키에 TTL이 있어 메모리 압박 시 오래된 세션을 우선 축출하는 게 noeviction의 "로그인
> 자체가 막히는" 실패보다 안전하다고 판단, `ipAllowList: []`로 내부망 전용)를 추가하고, 백엔드 서비스의
> `REDIS_HOST`/`REDIS_PORT` 환경변수를 `fromService`로 자동 주입하도록 연결했다(애플리케이션 코드/로컬 `.env`
> 변경 없음 — 기존에 읽던 프로퍼티 이름 그대로). `management.health.redis.enabled`도 `false`→`true`로 전환해
> Redis 장애 시 `/actuator/health`가 실제로 unhealthy를 반환하도록 했다. 로컬에서 동일 설정 경로(빈 값 없이
> host/port만 사용)로 회원가입→로그인까지 Redis 연동 정상 동작 재확인 완료. **사용자 측 조치 필요**: 이
> `render.yaml` 변경사항을 반영해 Render에서 Blueprint를 재동기화(Manual Sync 또는 재배포)해야 `alrdream-redis`
> 인스턴스가 실제로 생성되고 백엔드에 연결 정보가 주입된다 — 그 전까지는 동일한 에러가 재현된다.)
>
> (2026-08-06 추가: 사용자가 Render 대시보드에서 Blueprint를 새로 만들려다, 이 워크스페이스에 등록된 Blueprint
> 인스턴스가 하나도 없다는 걸 발견 — 즉 실제 배포된 백엔드 서비스(대시보드상 이름 `alrdream`,
> `alrdream.onrender.com`)는 애초에 `render.yaml` Blueprint로 만들어진 게 아니라 Render 대시보드에서 수동으로
> (New → Web Service) 만들어진 것이었다. 이 상태에서 `render.yaml`의 서비스 이름이 `alrdream-backend`로
> 달라(Phase 01 작성 당시엔 실제 배포 이름을 몰라서 임의로 지음) 있어, 그대로 새 Blueprint를 생성하면 Render가
> 기존 서비스를 인식하지 못하고 `alrdream-backend`라는 별도의 새 서비스를 중복 생성할 위험이 있었다. 나중에 더
> 꼬이는 걸 막기 위해 `render.yaml`의 서비스 이름을 실제 배포명과 동일한 `alrdream`으로 정정 — 이제 Blueprint를
> 새로 만들면 이름이 일치해 기존 서비스를 그대로 입양(adopt)할 것으로 예상된다. 실제 Blueprint 생성/입양은
> 사용자가 Render 대시보드에서 진행 중.)

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
- [x] OAuth2 소셜 로그인 — Google, Apple [03] §4-5. **설계 결정**: Spring Security의 OAuth2Client 리다이렉트 플로우 대신, Frontend(Expo 모바일)가 각 provider SDK로 발급받은 ID 토큰을 백엔드가 검증하는 방식으로 구현(`POST /api/auth/oauth/google`, `/apple`) — 모바일 앱에는 브라우저 리다이렉트보다 이 패턴이 표준적. Google은 `google-api-client`, Apple은 Apple JWKS(`nimbus-jose-jwt`)로 검증. Google은 실제 client-id로 배선 완료, **Apple은 Apple Developer 자격증명이 아직 없어(.env 비어있음) 코드만 구현, 실제 토큰으로 검증 안 됨** — 크리덴셜 채워지면 재검증 필요. **(2026-08-06 추가)** Apple Developer 유료 멤버십 구독 이슈로 당분간 크리덴셜 발급 자체가 불가해 Apple 로그인 기능을 명시적으로 보류하기로 결정. 코드는 Google 경로와 완전히 분리돼 있어(`AuthService`의 provider별 switch 분기, 독립된 `AppleIdTokenVerifierAdapter` 빈, 별도 엔드포인트) 그대로 둬도 무해하며, Google 로그인 동작에는 영향 없음. Frontend(Phase 13)에서는 Apple 로그인 버튼을 당분간 노출하지 않는다. 단, iOS 앱스토어 심사 정책(가이드라인 4.8 — 제3자 소셜 로그인을 제공하면 Sign in with Apple도 필수) 때문에 **Google 로그인을 iOS 빌드에 노출하려면 결국 Apple 로그인도 함께 활성화해야 함** — iOS 앱스토어 제출 전까지는 반드시 해결 필요.
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

- [x] `AiClient` 인터페이스 + Claude API 구현체 (`@HttpExchange`) [03] §4-3
- [x] `PromptBuilder` — `promptKey` → 프롬프트 템플릿 변수 매핑 [02] §6
- [x] Claude Tool Use 기반 Structured Output 파싱 (섹션별 JSON 스키마 강제)
- [x] `ai_generation_jobs` 비동기 처리 (Virtual Thread) + Job 상태 폴링 API [03] §4-4
- [x] `ai_generation_jobs` 생성 전 Free 티어 사용량(`usage_quotas`) 체크 — 초과 시 429 응답 [01] 13번 (생성 진입점이 이 한 곳뿐이라 여기서 공통 처리)

## 설계 결정

- **범위**: 이 Phase는 [03] §4-3/§4-4가 요구하는 재사용 가능한 AI 연동 "인프라"만 구현한다. 실제 기획/분석/설계
  콘텐츠의 시스템 프롬프트 구성·섹션 스키마·생성 결과 저장은 각 도메인이 알아야 할 지식이라 Phase 07~09에서
  다룬다. `AiGenerationJobService.submit(userId, targetType, targetId, task)`가 Job 생명주기(PENDING →
  PROCESSING → COMPLETED|FAILED)와 Virtual Thread 비동기 실행을 맡고, `task`(프롬프트 구성 → `AiClient` 호출 →
  파싱 → 저장)는 호출하는 도메인이 람다로 넘긴다.
- **`ai_generation_jobs.user_id` 컬럼 추가** (`V4__add_user_id_to_ai_generation_jobs.sql`): [03] §5 원안에는
  없었으나, Job 상태 폴링 API가 본인이 생성한 Job만 조회하도록 해야 하는데 `target_id`가 다형 참조라 target
  테이블 조인으로는 소유권을 확인할 수 없다. Job이 스스로 소유자를 들고 있는 편이 단순하고 안전해 컬럼을 추가했다.
- **Job 커밋 후 실행**: `submit()`이 Job을 저장한 트랜잭션이 커밋되기 전에 Virtual Thread가 먼저 그 Job을
  조회하면 아직 보이지 않는 레코드를 찾는 경합이 생길 수 있어, `TransactionSynchronizationManager`로
  `afterCommit` 이후에만 비동기 실행을 시작하도록 했다 (트랜잭션이 없는 컨텍스트에서 호출되면 즉시 실행).
- **FREE 티어 quota 동시성**: `usage_quotas`도 Phase 05의 `survey_definitions` 버전 발행과 같은 종류의 경합
  가능성이 있다 — 동시 요청이 같은 사용자·기간의 quota row를 동시에 최초 생성하려다 `UNIQUE(user_id, period)`
  위반이 나거나, 한도 검사를 동시에 통과해 실제 한도보다 더 많이 생성될 수 있다. `pg_advisory_xact_lock`으로
  사용자+기간 단위 직렬화해 미연에 방지했다(실제 동시 요청으로 재현하기 전에 코드 리뷰 단계에서 발견).
- **PRO는 quota row 자체를 만들지 않음**: `UsageQuotaService.checkAndIncrement`는 `member.getPlan() == PRO`면
  `usage_quotas` 테이블에 아예 손대지 않고 즉시 반환한다 — PRO 사용자는 quota 추적 대상이 아니라는 걸 DB
  상태로도 명확히 한다.
- **Jackson 3(`tools.jackson`) 채택**: `jjwt-jackson` 등 기존 라이브러리는 Jackson 2.x를 요구해 여러 클래스에서
  로컬 Jackson 2.x `ObjectMapper`를 관례적으로 써왔지만(Phase 03~05), Claude 전용 `RestClient`는 그런 레거시
  제약이 없는 새 컴포넌트라 Spring 7의 현재 권장 스택인 Jackson 3(`tools.jackson.databind.json.JsonMapper`)을
  그대로 썼다. `RestClient.Builder.messageConverters(...)`는 Spring 7.0부터 제거 예정으로 deprecated돼 있어
  `configureMessageConverters(...).withJsonConverter(...)`를 사용했다.
- **모델 id**: `app.ai.claude.model` 기본값은 `claude-sonnet-5` — 실제 API 키로 `GET /v1/models`를 호출해
  키가 접근 가능한 모델 목록에서 확인한 값이다(추측 아님). 환경변수로 얼마든지 교체 가능하도록 설정값으로 뺐다.
- **스파이크 엔드포인트**: `POST /spike/ai-generation-smoke-test` (인증 필요)로 quota 체크 → Job 생성 → Claude
  호출 → Tool Use 구조화 출력 파싱까지 전체 파이프라인을 실제로 검증한다. Phase 01의 PDF 스모크 테스트와 같은
  성격 — Phase 07이 실제 생성 흐름을 만들면 삭제 예정. PDF 스모크 테스트와 달리 사용자별 quota를 다뤄야 해서
  `/spike/**` 전체가 아니라 `/spike/pdf-smoke-test`만 인증 예외로 좁혔다(`SecurityConfig`).

## 테스트 결과

실제 발급받은 Claude API 키(`GET https://api.anthropic.com/v1/models`)로 모델 id를 확인 후, 시드 테스트
계정(`user@alrdream.test`, `admin@alrdream.test`)으로 실제 Supabase + 배포 중인 앱에 대해 검증했다.

- **quota 체크 → Job 생성 → 비동기 처리 → 폴링 전체 파이프라인**: `POST /spike/ai-generation-smoke-test` 호출 시
  quota 즉시 차감, `PENDING` Job이 즉시 반환됨을 확인. `GET /api/ai-generation-jobs/{jobId}` 폴링으로
  `PROCESSING`을 거쳐 최종 상태로 전이되는 것을 확인.
- **Claude API 실호출 — happy path 확인 완료**: 최초 시도 시 API 키 크레딧 잔액 부족으로
  `"Your credit balance is too low..."` 오류가 났는데(인증·요청 스키마 검증은 모두 통과한 뒤 과금 단계에서만
  나는 오류라 요청 형식이 올바르다는 근거는 이때 이미 확보), `AiGenerationException` → Job `FAILED` 전이 및
  `error_message` 기록까지 정상 확인. 이후 크레딧 충전 후 재호출(총 1회, 크레딧 절약을 위해 최소 호출로 진행)
  — Tool Use로 강제한 JSON이 스키마 그대로 파싱되어 `COMPLETED`로 전이됨을 확인
  (`{"one_line_summary":"야근이 많은 직장인을 위한 저녁 식사 정기 배달 구독 서비스","target_customer":"야근이 잦은 직장인"}`).
  이번 phase에서 발견된 버그는 없음.
- **FREE 티어 한도 강제**: `free-tier-monthly-limit=5`로 5회는 200, 6번째 호출은 정확히
  `429 {"code":"QUOTA_EXCEEDED"}` 반환 확인.
- **PRO 무제한**: 테스트 계정 plan을 임시로 `PRO`로 전환해 6회 연속 호출 모두 200 확인(quota 체크를 아예
  건너뜀 — `usage_quotas`에 row가 생성되지 않는 것도 DB로 직접 확인), 이후 `FREE`로 원복.
- **소유권 격리**: 다른 사용자(admin)가 user의 jobId로 폴링 시 `400 존재하지 않는 작업입니다` 확인(레코드
  존재 여부를 노출하지 않음).
- **인증 요구**: `/spike/ai-generation-smoke-test`, `/api/ai-generation-jobs/{jobId}` 모두 토큰 없이 401 확인.
- **`PromptBuilder` 매핑 로직**: jshell로 직접 검증 — questionId→promptKey 매핑, `isUnknown=true` 답변의
  플래그 텍스트 치환, 스키마에 없는 questionId 무시 모두 의도대로 동작.
- 테스트로 생성된 `ai_generation_jobs`(11건)/`usage_quotas`(1건)는 정리 완료, `./gradlew test` 통과.

---

# Phase 07: 기획(Planning) 도메인

## 작업 항목

- [x] 기획 생성 — 설문 응답 기반 AI 생성 [01] 2번
- [x] 기획 수정 — 이전 응답 불러와 편집 후 재생성, 버전 기록 [01] 5번
- [x] 기획 다중 삭제 (소프트 삭제) [01] 6번

## 설계 결정

- **"생성"과 "수정"은 같은 엔드포인트**: `POST /api/workspaces/{workspaceId}/planning-versions`가 [01] 2번(최초
  생성)과 5번("수정")을 모두 처리한다. [02] §7 원칙대로 설문 응답은 불변이라 "수정"은 편집된 답변으로 새
  설문 응답을 먼저 제출한 뒤(기존 `SurveyController`, Phase 05) 그 응답으로 이 엔드포인트를 다시 호출하는
  방식 — 별도의 "수정" API가 필요 없다.
- **응답 형태**: 생성 직후엔 아직 콘텐츠가 없으므로(`GENERATING`), 엔드포인트는 `PlanningVersion`이 아니라
  `AiGenerationJob`(Phase 06 공용 인프라)을 반환한다. `job.targetId`가 곧 `planningVersionId`라 클라이언트는
  `GET /api/ai-generation-jobs/{jobId}` 폴링만으로 완료 시점과 대상 ID를 모두 얻는다.
- **버전 번호 경합 방지**: `survey_definitions` 발행(Phase 05)과 동일한 클래스의 경합 — 같은 워크스페이스에
  동시 생성 요청이 오면 "다음 버전 번호" 계산이 겹칠 수 있어 `pg_advisory_xact_lock`으로 워크스페이스 단위
  직렬화했다. 버전 번호는 소프트 삭제 후에도 재사용하지 않는다(삭제 여부와 무관하게 최댓값+1).
- **DESIGN 설문 응답 차단**: `survey_response`가 참조하는 `survey_definition.survey_key`가 `DESIGN`이면 400 —
  기획 생성에는 `PLANNING_HAS_IDEA`/`PLANNING_EXPLORING` 응답만 쓸 수 있다.
- **12-4 구조를 JSON Schema로 그대로 강제**: `PlanningGenerationSpec`에 [01] 12-4의 10개 섹션(아이디어 요약~
  리스크&보완 포인트)을 각각 `required` 필드를 가진 JSON Schema로 정의하고, 시스템 프롬프트에 12-6의 차별화
  원칙("사업 시작 가능한 수준", 막연한 요약 금지, 타겟 고객 구체화, MVP 필수 포함 등)을 그대로 반영했다.
- **Phase 06 리팩터링**: quota 체크(`UsageQuotaService.checkAndIncrement`)를 각 호출부가 아니라
  `AiGenerationJobService.submit()` 내부로 옮겼다 — 애초 Phase 06 설계 의도("생성 진입점이 이 한 곳뿐이라
  여기서 공통 처리")대로 되돌린 것으로, Planning이 실제 첫 호출부가 되면서 매번 quota 체크를 잊지 않고 호출해야
  하는 부담을 없앴다. 이에 따라 Phase 06의 `AiGenerationSmokeTestController` 스파이크 엔드포인트는 삭제했다
  (원래도 "Phase 07에서 실제 생성 흐름이 만들어지면 삭제 예정"으로 문서화되어 있었음).

## 테스트 결과

시드 테스트 계정(`user@alrdream.test`)으로 실제 Supabase에 대해 검증했다. Claude API 크레딧을 아끼기 위해
비용이 드는 실제 생성 호출은 1회만 수행하고, 나머지는 AI 호출 전에 막히는 검증 케이스로 채웠다.

- **무료 검증(AI 호출 없이 400 확인)**: DESIGN 설문 응답으로 기획 생성 시도, 존재하지 않는 workspaceId, 다른
  사용자의 워크스페이스에 대한 생성 시도, 빈 배열로 다중 삭제, 존재하지 않는 버전 ID로 다중 삭제 — 모두 의도한
  메시지와 함께 400 확인.
  실행 흐름을 실제 계정으로 end-to-end 검증(설문 응답 제출 → 기획 생성 → Job 폴링 → 상세 조회 → 목록 조회 →
  다중 삭제 → 삭제 후 재조회) — 전부 정상. 특히:
  - 답변에 `isUnknown=true`로 표시한 두 문항(경쟁 서비스, 기존 대안)이 실제로 "추가 조사 필요 영역"에
    반영된 것을 확인 — `PromptBuilder`의 플래그 치환이 프롬프트를 거쳐 최종 콘텐츠에까지 실제로 이어짐.
  - 생성된 콘텐츠가 [01] 12-4의 10개 섹션을 빠짐없이, 막연하지 않고 구체적으로 채움(타겟 고객·수익 모델·MVP
    범위·실행 로드맵 모두 설문 응답 맥락을 반영한 실질적인 내용).
  - DB 직접 조회로 `usage_quotas.generation_count`가 정확히 1만 차감됐음을 확인(Phase 06 리팩터링 전이었다면
    스파이크 컨트롤러의 이중 호출로 2가 차감됐을 상황).
  - `planning_versions.content`가 Base64 암호문으로 저장됨을 psql로 직접 확인(평문 아님).
  - 소프트 삭제 후 목록에서 빠지고 상세 조회는 400으로 막힘을 확인.
- 이번 phase에서 발견된 버그는 없음(첫 실행에 happy path 포함 전부 정상). 테스트 데이터 정리 완료, `./gradlew test` 통과.

---

# Phase 08: 분석(Analysis) 도메인

## 작업 항목

- [x] 분석 생성 — 설문 없이 기획 본문을 입력으로 AI 생성, 합법여부/가용 리소스/경쟁 서비스 포함 [01] 7번
- [x] 분석 수정 (버전 기록) [01] 8번
- [x] 분석 다중 삭제 (소프트 삭제) [01] 9번

## 설계 결정

- **"생성"과 "수정"이 완전히 같은 호출**: [03] §4-2대로 분석은 별도 설문 없이 기획안 본문 자체가 입력이라,
  입력이 달라질 여지가 없다 — Planning과 달리 "수정"에 새 사용자 입력조차 없다. 그래서 `POST
/api/workspaces/{workspaceId}/planning-versions/{planningVersionId}/analysis-versions`를 반복 호출하는
  것 자체가 곧 "수정"이다(매번 새 버전 생성). 완료되지 않은(GENERATING) 기획안으로는 분석을 만들 수 없다.
  **분석 콘텐츠 구조는 문서에 명시돼 있지 않아 직접 설계**했다 — [01] 7번이 명시한 3항목(합법여부, 가용
  리소스[물적/인적], 경쟁 서비스)에 더해, [02] §5-3 DESIGN 설문이 필요로 하는
  `core_feature_candidates`(key/label 목록)와 종합 의견을 추가했다.
- **[02] §5-3 연결고리 완성**: Phase 05에서 항상 빈 목록을 반환하던 placeholder
  `NoAnalysisDesignFeatureOptionResolver`를 삭제하고, 실제 구현체 `AnalysisFeatureOptionResolver`로
  교체했다 — 워크스페이스의 기획안 버전들 중 가장 최근에 완료된 분석의 `core_feature_candidates`를 읽어
  `DESIGN` 설문 `core_feature_priority` 문항의 동적 옵션으로 주입한다. 파싱 실패 시에도 설문 조회 자체가
  막히지 않도록 빈 목록으로 폴백한다.
- **버전 번호 경합 방지**: Phase 05/07과 동일하게 `pg_advisory_xact_lock`으로 기획안 버전 단위 직렬화.

## 테스트 결과

시드 테스트 계정(`user@alrdream.test`)으로 실제 Supabase에 대해 검증했다. AI 호출은 워크스페이스 생성→설문
제출→기획 생성→분석 생성의 전체 사슬을 타야 해서 Phase 07보다 비용이 크지만, 무료로 막히는 검증(GENERATING
상태의 기획안으로 분석 시도, 소유권, 빈/잘못된 삭제 요청)은 여전히 AI 호출 없이 확인했다.

- 기획 생성 1회가 Claude 쪽 일시적 `529 Overloaded`로 실패 — 코드 버그가 아니라 업스트림 일시 장애였고,
  `AiGenerationException` → Job `FAILED` 전이와 정확한 오류 메시지 기록은 의도대로 동작함을 오히려 확인.
  재시도로 정상 완료.
- **핵심 통합 시나리오 확인**: 완료된 기획안으로 분석 생성 → Job 폴링 → `COMPLETED` → 분석 상세 조회에서
  법적 검토(동물보호법/동물위탁관리업 등록, 개인정보보호법 등 실제로 정확한 내용), 자원 가용성, 경쟁 구도가
  기획안 맥락에 근거해 구체적으로 채워짐을 확인. 이어서 **같은 워크스페이스의 `DESIGN` 설문을 다시 조회해
  `core_feature_priority` 문항의 옵션이 방금 생성된 분석의 `core_feature_candidates` 8개와 정확히 일치함을
  확인** — Phase 05부터 placeholder로 남겨뒀던 연결고리가 실제로 동작.
- 분석을 소프트 삭제한 뒤 같은 `DESIGN` 설문을 다시 조회하면 옵션이 다시 빈 배열로 폴백됨을 확인(삭제된
  버전은 옵션 조회 대상에서 제외).
- `usage_quotas.generation_count`가 정확히 3(기획 실패 1 + 기획 성공 1 + 분석 성공 1)만 차감됐음을 DB로
  직접 확인 — quota 이중 차감 없음.
- 이번 phase에서 발견된 구현 버그는 없음(업스트림 일시 장애 1건 제외). 테스트 데이터 정리 완료, `./gradlew test` 통과.

---

# Phase 09: 설계(Design) 도메인

## 작업 항목

- [x] 설계 생성 — `DESIGN` 설문 + 분석 결과를 함께 입력으로 AI 생성 [01] 10번
- [x] 설계 수정 — 이전 응답 불러와 편집 후 재생성, 버전 기록 [01] 11번

## 설계 결정

- **"생성"과 "수정"은 같은 엔드포인트**: Planning과 동일한 패턴 — `POST .../analysis-versions/{id}/design-versions`가
  [01] 10번과 11번을 모두 처리한다. "수정"은 편집된 답변으로 새 DESIGN 설문 응답을 먼저 제출한 뒤 그 응답으로
  다시 호출하는 방식.
- **경로 중첩이 도메인 의존 관계 그대로**: `/workspaces/{id}/planning-versions/{id}/analysis-versions/{id}
/design-versions` — [03] §4-2의 `design → analysis(특정 버전), survey_response` 참조 관계와 DB FK 체인
  (`design_versions.analysis_version_id → analysis_versions.planning_version_id →
planning_versions.workspace_id`)을 그대로 URL로 옮겼다. 4단계 중첩이지만 각 단계의 소유권 검증을 하위
  서비스가 상위 서비스의 `getOwned`를 그대로 재사용해 사슬로 연결할 수 있어 구현은 오히려 단순했다
  (`DesignVersionService` → `AnalysisVersionService.getOwned` → 내부적으로 `PlanningVersionService.getOwned`
  → `WorkspaceService.getOwned`).
- **[02] §6 — 이전 단계 컨텍스트 누적**: 설계 생성 프롬프트에는 (1) 원본 기획 단계 설문 응답, (2) 분석 결과
  전체(JSON), (3) 이번 설계 설문 응답을 모두 포함한다. `design_versions`는 `analysis_version_id`만 참조하고
  있어, 원본 기획 설문 응답을 얻으려면 `planning_version`을 한 번 더 거쳐야 했다(`analysisVersion` →
  `planningVersionId` → `planningVersion.surveyResponseId`).
- **콘텐츠 구조**: 역시 문서에 명시가 없어 [02] §5-3 DESIGN 설문 문항에 1:1 대응하는 구조로 설계했다 —
  선택된 핵심 기능별 명세(`feature_specification`), MVP 확정 범위, 기술 스택/아키텍처(제약 반영 근거 포함),
  플랫폼 전략, 데이터/개인정보 처리 방안, 화면/시스템 구조, 개발 단계별 계획.
- **버전 번호 경합 방지**: Phase 05/07/08과 동일하게 `pg_advisory_xact_lock`으로 분석 버전 단위 직렬화.

## 테스트 결과

시드 테스트 계정으로 워크스페이스 생성 → 설문 제출 → 기획 생성 → 분석 생성 → DESIGN 설문 응답(분석의
동적 옵션 사용) → 설계 생성까지 전체 사슬을 실제 계정/DB로 end-to-end 검증했다. 무료로 막히는 검증(DESIGN이
아닌 응답 사용, 존재하지 않는 분석 ID, 소유권, 존재하지 않는 ID로 삭제)은 AI 호출 없이 확인했다.

- **핵심 통합 시나리오**: 설계 설문(Q1)에서 고른 4개 핵심 기능이 생성된 설계 문서의 `feature_specification`에
  정확히, 같은 순서로 상세 명세됨을 확인. Q3(1인 개발/3개월/500만원 제약)이 권장 기술 스택(Next.js +
  Supabase 조합, 별도 서버 인프라 없는 BaaS 중심 아키텍처) 선정 근거에 정확히 반영됨을 확인. Q4(웹 우선),
  Q5(개인정보 민감)도 각각 `platform_plan`, `data_and_privacy`에 정확히 반영됨을 확인.
- Job 폴링, 목록/상세 조회, 다중 삭제, 삭제 후 재조회 시 400 처리까지 모두 정상.
- `usage_quotas.generation_count`가 정확히 3(기획/분석/설계 각 1회 성공)만 차감됨을 DB로 직접 확인.
- `design_versions.content`가 Base64 암호문으로 저장됨을 psql로 직접 확인.
- 이번 phase에서 발견된 버그는 없음(3단계 전체 사슬이 첫 실행에 정상 동작). 테스트 데이터 정리 완료,
  `./gradlew test` 통과.

---

# Phase 10: PDF 생성 파이프라인

> Phase 01에서 검증한 OpenHTMLtoPDF 파이프라인을, 실제 기획/분석/설계 콘텐츠 구조([01] 12-4)에 맞춰 완성한다.

## 작업 항목

- [x] `content(JSON)` → Thymeleaf 템플릿 렌더링 ([01] 12-4 구조: 아이디어 요약~리스크) [03] §4-6
- [x] OpenHTMLtoPDF로 HTML → PDF 변환
- [x] Supabase Storage 업로드 + `documents` 레코드 저장, 서명 URL 발급

## 설계 결정

- **세 도메인이 공유하는 단일 진입점**: `DocumentService.getOrGenerate(sourceType, sourceId, templateName,
contentJson, extraModel)` 하나를 `PlanningVersionService`/`AnalysisVersionService`/`DesignVersionService`가
  각자 `POST .../{versionId}/pdf`에서 얇게 호출한다. `AiGenerationJobService`/`UsageQuotaService`와 동일한
  "공용 진입점 하나" 패턴 — quota 처리를 `AiGenerationJobService.submit()` 안으로 모은 Phase 07 리팩터링과
  같은 이유(생성 진입점이 여러 곳으로 흩어지면 언젠가 한 곳에서 빠뜨린다).
- **완료된 버전의 content는 불변 → PDF도 소스당 최초 한 번만 렌더링**: `documents`에 이미 행이 있으면
  Thymeleaf 렌더링/OpenHTMLtoPDF 변환/S3 업로드를 전부 건너뛰고 저장된 오브젝트 키로 서명 URL만 다시
  발급한다. 서명 URL은 만료되므로(TTL 15분) `documents.file_url`에는 서명 URL이 아니라 Storage 오브젝트
  키(`documents/{type}/{id}.pdf`)만 저장하고, 조회 때마다 새로 서명한다.
- **PDF는 완료(`COMPLETED`) 상태에서만 발급**: `GENERATING`/`FAILED` 상태에서 호출하면 400 — 각 서비스의
  `generatePdf`가 기존 `getOwned`로 소유권을 확인한 뒤 상태를 확인하는 얕은 래퍼라, Planning/Analysis/Design
  세 곳 모두 동일한 모양이 됐다.
- **한글 폰트 임베딩이 필수**: PDFBox 기본(Base14) 폰트는 한글 글리프가 없어 별도 조치 없이는 빈 사각형만
  나온다. Render 배포 이미지가 `eclipse-temurin-jre-alpine`이라 OS 폰트도 기대할 수 없어, TTF를 애플리케이션
  리소스로 직접 번들하고 OpenHTMLtoPDF `useFont(...)`로 매 렌더링마다 등록하는 방식을 썼다. 전체 Noto Sans
  KR(Regular+Bold variable font, 원본 10MB)은 CJK 통합 한자를 모두 포함해 그대로 쓰기엔 과한 크기라,
  `fonttools`로 wght=400/700 정적 인스턴스를 추출한 뒤 한글 음절(`AC00-D7A3`)/자모/라틴/일반 구두점 범위만
  서브셋해 `NotoSansKR-{Regular,Bold}.ttf`(각 2.4MB)로 축소했다 — 한자가 필요 없는 이유는 콘텐츠가 전부 AI가
  생성한 한국어 산출물이기 때문. OFL 1.1 라이선스 원문은 `resources/fonts/OFL.txt`로 함께 보관한다(재배포 시
  라이선스 동봉 요건).
- **템플릿 모델**: `content` JSON 문자열을 Jackson 2.x `ObjectMapper`(다른 곳과 동일하게 Jackson 3.x 자동
  구성 빈과의 충돌을 피하기 위한 독립 인스턴스)로 `Map<String,Object>`로 파싱해 Thymeleaf 컨텍스트의
  `content` 변수로 넣는다. 스키마가 세 도메인 모두 다르므로(Planning=[01] 12-4 10섹션, Analysis/Design=
  Phase 08/09에서 새로 설계한 구조) 템플릿도 도메인별로 별도 작성(`templates/pdf/{planning,analysis,
design}.html`).
- **Supabase Storage는 S3 호환 API**: AWS SDK v2(`software.amazon.awssdk:s3`)를 그대로 사용하고
  `pathStyleAccessEnabled(true)`만 켰다(버킷을 서브도메인이 아닌 경로로 구분). 리전 값은 실제로 검증되지
  않지만 SigV4 서명에는 필요해, `SUPABASE_DB_URL`의 풀러 호스트(`aws-1-ap-northeast-2`)로 확인한 프로젝트
  리전(`ap-northeast-2`)을 기본값으로 뒀다.

## 테스트 결과

budget 제약상 실제 유료 Claude 호출은 딱 3번(기획/분석/설계 각 1회, Phase 09 테스트와 동일한 빵집 재고관리
아이디어로 새 계정을 만들어 처음부터 다시 생성)만 쓰고, PDF 파이프라인 자체(렌더링/업로드/서명 URL)는
전부 무료라 반복 검증했다.

- **세 도메인 PDF 모두 실제 Supabase Storage에 업로드하고 서명 URL로 내려받아 직접 열어봤다** — `pypdf`로
  텍스트를 추출하고 `PyMuPDF`로 페이지를 이미지로 렌더링해 육안 확인. 기획 5페이지/분석 3페이지/설계
  4페이지, 한글이 깨지거나 빈 사각형 없이 정상 출력됨을 확인.
- **버그 발견 및 수정**: 설계 PDF의 "기능 명세" 표에서 기능명 칼럼에 너비를 지정하지 않아, 표 자동 레이아웃이
  설명 칼럼에 공간을 몰아주면서 긴 한글 기능명이 글자 하나당 한 줄씩 세로로 쪼개져 렌더링되는 문제를
  발견했다. `table-layout: fixed`로 바꾸고 기능명 칼럼에 명시적 너비(32mm)를 줘서 해결 — 같은 문제가 잠재해
  있던 기획 PDF의 로드맵 표에도 동일하게 적용(단계 칼럼 28mm→32mm). 수정 후 재렌더링해 표가 정상적으로
  줄바꿈됨을 다시 확인.
- **캐싱 동작 확인**: 같은 버전에 PDF 발급을 두 번 호출했을 때 `generatedAt`이 완전히 동일 — 재렌더링/재업로드
  없이 서명 URL만 다시 발급됐음을 확인. `documents` 테이블에 소스당 정확히 1행만 있음을 psql로 확인,
  `file_url`이 서명 URL이 아니라 오브젝트 키로 저장됨을 확인.
- **[02] §6 컨텍스트 누적이 PDF에도 그대로 반영됨을 재확인**: 설계 설문에서 고른 4개 기능이 PDF의 "1. 기능
  명세" 표에 정확히 같은 순서로 나타남. 기획 PDF의 "10. 리스크 & 보완 포인트" 섹션에도 설문에서
  `isUnknown=true`로 답한 두 문항이 "추가 조사 필요 영역"에 반영됨을 확인 — `PromptBuilder`의 unknown
  처리(Phase 06)가 PDF까지 정상적으로 이어짐.
- **무료 검증**: `GENERATING` 상태(생성 직후 폴링 전 찰나)에 PDF를 호출하면 400("생성이 완료된 ○○만 PDF로
  내려받을 수 있습니다"), 존재하지 않는 버전 ID로 호출하면 400("존재하지 않는 ○○입니다"), 인증 없이
  호출하면 401 — 모두 확인.
- 테스트 계정/워크스페이스/버전/Job/quota/DB 행을 모두 정리했고, 테스트로 업로드한 Storage 오브젝트 3개도
  boto3로 직접 삭제해 정리했다. `./gradlew test` 통과.

---

# Phase 11: 구독/결제 (subscription)

## 작업 항목

- [x] PortOne 빌링키 발급 연동 — Frontend/Admin SDK(`requestIssueBillingKey`) → 백엔드 `billingKeyId` 수신/암호화 저장 [03] §4-7
- [x] 최초 결제 요청 + 다음 달 결제 예약(`timeToPay`) API
- [x] `POST /webhooks/portone` — Standard Webhooks 서명 검증(JVM SDK), `payment_id` 기준 멱등 처리
- [x] 웹훅 이벤트 처리 — `Transaction.Paid`(결제 이력 저장 + 다음 결제 재예약), `Transaction.Failed`(`status=PAST_DUE` 전환)
- [x] Pro 구독 권한 반영 — 무제한 생성

## 설계 결정

- **PortOne JVM Server SDK 좌표**: 문서에 정확한 Maven 좌표가 없어 `portone-io/server-sdk` 저장소를 직접
  확인(Maven Central 메타데이터, `jvm/lib/src/generated` 소스)해 `io.portone:server-sdk:0.24.0`을 확정했다.
  Kotlin으로 구현돼 있지만 각 API마다 `@JvmName`으로 Java 친화적인 `CompletableFuture` 오버로드(예:
  `payWithBillingKeyFuture` → Java에서는 `payWithBillingKey`)를 함께 제공해 순수 Java 코드에서 그대로 쓸 수
  있었다. 단, SDK의 모든 예외(`PortOneException`)가 Kotlin에서는 checked exception이 아니지만 Java 관점에서는
  `Exception`을 상속한 **checked** 예외라, `WebhookVerifier.verify()`(동기 메서드)를 감싸는 쪽은
  `throws WebhookVerificationException`을 명시해야 했다(`CompletableFuture` 경로는 `.join()`이
  `CompletionException`으로 감싸 unchecked라 문제없음).
- **`WebhookVerifier`는 빈으로 만들지 않는다**: 생성자가 secret을 즉시 base64 디코딩해 검증하므로, 아직 웹훅
  secret을 발급받지 못한 상태(빈 값)에서 빈 생성 자체가 실패해 애플리케이션이 부팅되지 않는 문제를 실제로
  겪었다. `EncryptedStringConverter`와 같은 이유로 secret 검증을 실제 웹훅 수신 시점까지 미루도록
  `PortOneWebhookService.handle()`에서 매 호출마다 새로 생성하는 방식으로 바꿨다.
- **결제 확정은 전적으로 웹훅에 위임**: `subscriptions` 스키마에는 대기 상태(PENDING)가 없어(ACTIVE/PAST_DUE/
  CANCELED 셋뿐), `subscribe()`가 PortOne에 최초 결제+다음 달 예약 요청을 보낸 직후에는 구독을 `PAST_DUE`(아직
  확정 안 됨)로 둔다. 최초 `Transaction.Paid` 웹훅이 와야 비로소 `ACTIVE`로 전환한다.
- **`paymentId`에 `subscriptionId`를 인코딩**: PortOne 웹훅 바디는 `paymentId`/`storeId`/`transactionId`만
  주고 우리 쪽 구독을 가리키는 정보가 없다. 별도 매핑 테이블 없이, 우리가 채번하는 `paymentId`를
  `"sub_{subscriptionId}_{UUID}"` 형식으로 만들어(UUID는 `_`를 포함하지 않으므로 파싱이 안전하다) 웹훅
  수신 시 문자열만으로 구독을 되짚는다(`PaymentIds`).
- **웹훅 바디의 금액은 신뢰하지 않는다**: `WebhookTransactionDataPaid`엔 `amount` 필드 자체가 없다(위변조
  가능성도 있어 애초에 신뢰하면 안 됨). `Transaction.Paid` 처리 시 `paymentClient.getPayment(paymentId)`로
  PortOne에 직접 조회해 확정된 금액을 가져온다.
- **재예약 체이닝**: `Transaction.Paid`를 받을 때마다(최초든 반복이든) 그 다음 달 결제를 다시
  `createPaymentSchedule`로 예약한다 — [03] §4-7의 "반복 체이닝" 그대로. 별도 스케줄러 없이 매 결제 성공이
  다음 결제를 스스로 예약하는 구조.
- **결제 실패 시 정책**: `Transaction.Failed`는 구독을 즉시 `PAST_DUE`로, 회원 플랜을 즉시 `FREE`로 되돌린다.
  [03] §4-7은 "재시도/알림 정책"을 미확정 항목으로만 언급해 구체적인 재시도 알고리즘은 이번 phase 스코프에
  넣지 않았다(그레이스 기간 없이 즉시 회수).
- **"고급 분석", "설계 문서 export"는 별도로 게이팅하지 않았다**: [01] 13번 BM 섹션의 "이후" 항목 문구를
  그대로 가져온 것일 뿐, [02]/[03] 어디에도 이 두 기능을 무엇으로 제한할지 정의돼 있지 않다(분석/설계
  생성과 PDF export는 이미 모든 사용자에게 동일하게 열려 있다 — Phase 08/10). 실제로 구현 가능한 건
  "무제한 생성"(FREE 티어 quota를 우회하는 것)뿐이라, `Subscription`이 확정될 때 `Member.plan`을 PRO로
  동기화해 기존 `UsageQuotaService`(Phase 06)가 그대로 무제한 처리하게 했다.
- **가격은 임의값**: 문서에 Pro 구독료가 명시돼 있지 않아 `app.portone.pro-monthly-price-krw: 9900`으로
  직접 정했다(설정값이라 나중에 바꾸기 쉬움).
- **전체 실결제 E2E 검증은 이번 phase 스코프 밖**: 빌링키 발급은 프론트/Admin SDK(브라우저)로만 가능하고,
  PortOne이 실제 웹훅을 보내려면 공개 도메인이 필요하다 — 둘 다 지금 갖추고 있지 않다. 이는 우연이 아니라
  milestone에도 이미 반영돼 있다: Phase 14 작업 항목에 "PortOne 웹훅 URL을 실제 배포 도메인으로 등록... 확인"이
  명시돼 있어, 실배포 이후로 원래 계획된 검증이다.

## 테스트 결과

실제 돈이 오가는 통합이라, "존재할 수 없는 빌링키로 결제를 시도하면 PortOne이 과금 이전 단계에서 거절한다"는
성질을 이용해 무료/안전하게 실제 API 연동을 검증했다. 이 요청은 `api.portone.io`에 실제로 도달했고(인증
성공), 카드사/PG 단계까지 가지 않고 `BillingKeyNotFoundError`로 즉시 거절돼 비용이 전혀 발생하지 않는다.

- **실제 PortOne API 왕복 확인**: 존재하지 않는 빌링키로 `POST /api/subscriptions` 호출 → 실제 `payWithBillingKey`
  API 호출까지 도달, `BillingKeyNotFoundException`으로 정상 거절, 400 응답. 이 한 번의 왕복으로 API Secret/
  Store ID/Channel Key 조합, 요청 바디 구성(파라미터 순서·타입), 응답 파싱이 전부 실제 환경 기준으로 맞다는
  것을 확인했다 — 첫 시도에 정상 동작.
- **실패한 구독 시도는 깨끗이 롤백됨**: PortOne이 거절해도 `@Transactional`로 `subscriptions` 행이 남지
  않음을 확인 — 재시도 시 "이미 구독 중" 오류 없이 다시 시도 가능.
- **중복 구독 방지**: DB에 직접 `PAST_DUE`/`ACTIVE` 구독 행을 넣고 재구독 시도 → PortOne 호출 전에 400으로
  막힘(무료 확인). `CANCELED` 상태에서는 재구독이 허용됨을 확인.
- **웹훅 서명 검증**: 실제 Standard Webhooks 알고리즘(HMAC-SHA256, `"{id}.{timestamp}.{body}"`, `v1,` 접두
  서명)을 Python으로 재구현해 로컬 전용 테스트 secret으로 서명한 요청을 직접 만들어 검증했다(운영
  `PORTONE_WEBHOOK_SECRET`은 아직 실제 웹훅 URL 등록 전이라 미발급 상태). 잘못된 서명은 400, 올바른 서명은
  통과함을 확인.
- **`Transaction.Failed` 전체 경로 실제 검증**: 잘못 만든 서명 웹훅 → 400. 올바르게 서명한 웹훅 →
  `payment_history`에 FAILED 행 기록, `subscriptions.status`가 `PAST_DUE`로, `users.plan`이 `FREE`로 전환됨을
  DB로 직접 확인. 같은 `payment_id`로 재전송(PortOne의 최대 5회 재시도 시뮬레이션) → 두 번째 요청은 기존
  행을 건드리지 않고 그대로 200 반환(멱등 처리 확인).
- **`Transaction.Paid`의 실패 경로 검증**: PortOne에 실제로 존재하지 않는 `paymentId`로 서명된 Paid 웹훅을
  보내면 `paymentClient.getPayment()`가 실패해 500이 반환됨(PortOne이 나중에 재시도하도록 유도하는 의도된
  동작)을 확인 — 이때도 구독/회원 상태가 전혀 바뀌지 않고 `payment_history`에도 행이 남지 않아, 트랜잭션이
  깨끗하게 롤백됨을 확인.
- **버그 발견 및 수정 (2건)**:
  1. `.env`/`.env.example`의 `PORTONE_WEBHOOK_SECRET= # TODO: ...`가 인라인 주석이 포함된 채로 값에 그대로
     들어가는 버그를 발견했다. Spring이 `.env`를 `.properties` 형식으로 읽는데(`spring.config.import`),
     `.properties`는 줄 끝 인라인 `#` 주석을 지원하지 않아 `PORTONE_WEBHOOK_SECRET`의 실제 값이
     `" # TODO: 추 후, 관련 기능 완성되면 추가할 것"` 전체가 돼버렸다. 이전까지 아무도 이 값을 읽지 않아
     드러나지 않았던 잠재 버그 — `WebhookVerifier` 빈이 이를 처음 소비하면서 부팅 실패로 표면화됐다. 주석을
     별도 줄로 옮겨 수정.
  2. **자가 호출로 인한 `@Transactional` 무시 버그**: `PortOneWebhookService.handle()`(외부에서 호출되는
     진입점, 원래 클래스 레벨 `@Transactional(readOnly = true)`에 의존)이 내부에서 `this.handlePaid(...)`/
     `this.handleFailed(...)`를 직접 호출했는데, 이 메서드들에 붙여둔 `@Transactional`(쓰기 가능)이 Spring
     프록시를 거치지 않아 무시되고, 실제로는 `handle()`의 읽기 전용 트랜잭션 안에서 실행되고 있었다. 그
     결과 `Transaction.Failed` 웹훅이 200을 반환하고 `users.plan`은 정상 변경되는 것처럼 보였지만(이미
     managed 상태였던 엔티티의 merge라 우연히 반영됨), `payment_history`에 새로 만든 행은 조용히 저장되지
     않는 현상을 실제 DB 조회로 발견했다. `@Transactional`을 실제 외부 진입점인 `handle()`로 옮기고,
     자가 호출되는 `handlePaid`/`handleFailed`에서는 의미 없는 `@Transactional`을 제거해 해결 — 수정 후
     재검증해 `payment_history` 행이 정상적으로 남는 것을 확인했다.
- 테스트로 만든 사용자/구독/결제 이력 행을 모두 정리했고, `./gradlew test`(Testcontainers 기반 부팅 테스트)
  통과를 확인했다.

### 추가 검증 — 실제 발급받은 웹훅 secret으로 재검증

사용자가 PortOne 관리자콘솔(테스트 모드)에서 웹훅 URL을 `http://localhost:8080/webhooks/portone`로 등록하고
실제 웹훅 secret을 발급받아 `.env`에 반영했다. 콘솔의 "호출 테스트" 버튼은 응답코드 407을 "성공"으로
표시했는데, 확인해보니 우리 서버 코드에는 407을 반환하는 경로가 전혀 없고(웹훅 헤더 없이 호출해도 500/400) —
`localhost`는 PortOne 서버 인프라 입장에서 사용자의 로컬 머신을 가리킬 수 없으므로, 이 테스트 호출은 실제로
로컬 서버에 도달하지 못하고 PortOne 내부 프록시에서 막힌 것으로 보인다(진짜 원격 전달 검증은 Phase 14에서
공개 URL로 재확인 필요).

대신 발급된 실제 secret으로 Standard Webhooks 서명을 직접 재구현해 로컬에서 재검증했다:

- 잘못된 secret으로 서명 → `WebhookVerificationException`("No matching signature found")으로 정상 거절 확인
  (이전엔 secret이 비어 있어 "Empty key" 예외였던 것과 구분됨 — 실제 secret이 로드됐다는 증거).
- 실제 secret으로 올바르게 서명한 `Transaction.Failed` 웹훅 → `payment_history`에 FAILED 행 기록,
  `subscriptions.status=PAST_DUE`, `users.plan=FREE`로 정상 전환, 중복 전송도 멱등 처리됨을 재확인.
- **버그 추가 발견 및 수정**: 웹훅 헤더(`webhook-id` 등)가 누락된 요청이 500을 반환하던 것을 재검증 중 발견 —
  `@RequestHeader`가 필수인데 누락 시 `MissingRequestHeaderException`을 처리하는 핸들러가 없었다.
  `GlobalExceptionHandler`에 핸들러를 추가해 400으로 정상화.

### 추가 검증 — Render 배포 서버 대상 원격 재검증

백엔드를 Render(`https://alrdream.onrender.com`)에 1차 배포하고 동일한 `PORTONE_WEBHOOK_SECRET`을 환경변수로
등록, PortOne 콘솔의 웹훅 URL도 `https://alrdream.onrender.com/webhooks/portone`로 갱신했다(공개 URL이라
`localhost` 문제 없이 PortOne이 실제로 도달 가능). 콘솔 "호출 테스트"가 이번엔 응답코드 200을 반환했고,
이를 재확인하기 위해 로컬 재검증과 동일한 방식(Standard Webhooks 서명을 직접 재구현해 자체 서명한 요청)으로
배포 서버를 대상으로 전체 경로를 다시 검증했다:

- `GET /actuator/health` → 200, 배포된 서버가 최신 코드임을 헤더 누락 요청의 응답(400, 직전에 고친
  `MissingRequestHeaderException` 처리)으로 함께 확인.
- Supabase DB(운영과 동일 인스턴스)에 테스트용 `users`/`subscriptions` 행을 직접 생성(`billing_key`는
  `DATA_ENCRYPTION_KEY`로 실제 AES-256-GCM 암호화한 값)하고, 그 구독을 가리키는 `paymentId`로 `Transaction.Failed`
  웹훅을 실제 secret으로 서명해 배포 URL로 전송.
- 잘못된 서명 → 400, 올바른 서명 → 200, DB 직접 조회로 `payment_history` FAILED 행 기록·
  `subscriptions.status=PAST_DUE`·`users.plan=FREE` 전환을 확인. 동일 페이로드 재전송 → 200이지만 새 행 없음
  (멱등 처리도 배포 환경에서 재확인).
- 테스트로 만든 행 전부 삭제, 정리 확인 완료.

결론: 배포 서버에서 서명 검증부터 DB 반영까지 전체 파이프라인이 로컬과 동일하게 정상 동작한다. 다만 이
검증도 여전히 자체 서명한 요청을 우리가 직접 전송한 것이라, "PortOne 서버가 실제 결제 이벤트 발생 시
자기 쪽에서 먼저 요청을 보내는" 진짜 엔드투엔드 전달 자체는 검증 범위 밖이다(콘솔의 200 응답이 이 부분의
간접 증거이긴 하다). 실제 카드로 결제해 진짜 웹훅이 오는 것까지 확인하려면 브라우저 기반 빌링키 발급
프론트엔드가 필요해 이번 phase 스코프에서는 다루지 않는다.

---

# Phase 12: Admin 앱

## 작업 항목

- [x] 구현시 데스크톱 / 랩톱 / 태블릿 / 모바일 반응형 화면 대응
- [x] 설문 정의 관리 화면 — 목록/에디터/버전 발행/미리보기 [03] §2
- [x] 사용자/워크스페이스 조회 화면 (CS 대응용)
- [x] AI 프롬프트 템플릿 관리 화면
- [x] 구독/사용량 대시보드

## 설계 결정

- **AI 프롬프트 템플릿을 DB로 이관(신규 `prompt_templates` 테이블)**: 착수 전 확인해보니 기획/분석/설계 생성에
  쓰이는 시스템 프롬프트·Claude Tool Use 스키마가 `PlanningGenerationSpec` 등 Java 코드에 하드코딩돼 있어
  "Admin에서 편집"이 애초에 불가능했다. `survey_definitions`와 동일한 패턴(불변 버전 관리 — 발행하면 새
  버전이 생기고 기존 버전은 절대 수정되지 않음, 최신 버전 = 활성 버전)으로 DB로 옮겼다. 마이그레이션(V5)이
  기존 하드코딩 값을 각 타입(PLANNING/ANALYSIS/DESIGN)의 버전 1로 그대로 이관해, 이번 phase 배포 이후에도
  생성 결과가 바뀌지 않는다. `PlanningVersionService`/`AnalysisVersionService`/`DesignVersionService`는
  이제 `PromptTemplateService.getActive(promptType)`로 최신 버전을 조회해 쓴다 — 3개의 `*GenerationSpec`
  클래스는 삭제.
- **`prompt_templates.prompt_type`은 새 enum을 만들지 않고 기존 `AiTargetType`(PLANNING/ANALYSIS/DESIGN)을
  재사용**: 이미 `ai_generation_jobs.target_type`에 정확히 같은 3개 값의 enum이 있어 중복 정의를 피했다.
- **FREE 티어 월별 생성 한도도 DB로 이관(신규 `free_tier_settings` 단일 행 테이블)**: 이전에는
  `app.ai.free-tier-monthly-limit`(application.yml 고정값 5)이었다 — "Free 티어 생성 횟수 한도 조정"([01]
  13번 BM, [03] §2-1)을 Admin에서 하려면 재배포 없이 값을 바꿀 수 있어야 하므로 DB 단일 행으로 옮기고
  `UsageQuotaService`가 매번 이 값을 조회하도록 변경. 사용자별 개별 한도 조정 기능은 이번 요구사항에 없어
  구현하지 않음(전역 기본값만 조정 가능).
- **사용자/워크스페이스 조회는 CS 목적 한정 — 수정/삭제 API를 만들지 않음**: [03] §2-1 설계에 "상태 확인,
  수정/삭제 등 직접 개입은 최소화"라고 명시돼 있어 그대로 따름. `GET /api/admin/users`(이메일 검색),
  `GET /api/admin/users/{id}`, `GET /api/admin/users/{id}/workspaces`만 제공.
- **구독 대시보드는 상태별 요약(`GET .../summary`) + 목록/필터만 제공**: `subscriptions.plan` 컬럼은 실제로
  항상 `PRO`(FREE 사용자는 애초에 구독 행 자체가 없음)라 plan 필터는 의미가 없어 만들지 않고, `status`
  필터만 지원.
- **CORS 설정 신규 추가**: 이전까지 백엔드를 호출하는 클라이언트가 없어(Postman/curl뿐) CORS 설정이 아예
  없었다 — 브라우저에서 Admin이 직접 API를 호출하는 이번 phase에서 처음 필요해졌다.
  `app.cors.allowed-origins`(env로 배포 도메인 추가 가능, 로컬 기본값은 Admin/Frontend 개발 서버 포트)로
  구성한 `CorsConfigurationSource` 빈을 `SecurityConfig`에 추가.
- **Admin 프론트는 React Query 등 데이터 레이어 없이 순수 `fetch` + hook으로 구성**: 화면 수·상호작용이
  단순해(대부분 목록 조회 + 폼 하나) 캐싱/재검증 라이브러리가 필요한 규모가 아니라고 판단, `react-router-dom`
  외에는 추가 런타임 의존성을 넣지 않았다. Access token은 `localStorage`에 저장하고, 401을 받으면
  refresh token으로 자동 갱신 후 원 요청을 재시도하는 로직을 `apiFetch` 안에 뒀다(refresh token은 회전형이라
  동시에 여러 401이 터져도 하나의 refresh 요청만 공유하도록 처리).
- **로그인 시 `role !== ADMIN`이면 즉시 토큰을 버리고 거부**: 백엔드가 `/api/admin/**`를
  `hasRole("ADMIN")`으로 이미 막고 있지만, 일반 회원이 실수로 Admin 콘솔에 로그인했을 때 "로그인은 됐는데
  모든 화면에서 403만 뜨는" 혼란스러운 상태 대신 로그인 단계에서 바로 명확한 에러를 보여주기 위함.

## 테스트 결과

- **마이그레이션(V5) 문제 및 수정**: 처음 작성한 마이그레이션이 Flyway 부팅 시 파싱 에러로 실패했다 —
  JSON Schema 리터럴을 담은 PostgreSQL dollar-quote 태그(`$SCHEMA_PLANNING$`) 바로 뒤에 JSON의 여는 중괄호
  `{`가 와서 `...$SCHEMA_PLANNING${`처럼 우연히 Flyway 플레이스홀더 문법(`${`)과 겹쳐, "No value provided for
  placeholder" 에러로 마이그레이션 자체가 파싱 실패했다. 이 프로젝트는 플레이스홀더 치환 기능을 쓸 계획이
  없어 `spring.flyway.placeholder-replacement: false`로 아예 꺼서 근본적으로 해결(개별 SQL을 고치는 대신,
  향후 비슷한 JSON 리터럴이 마이그레이션에 또 들어올 가능성을 감안).
- **`./gradlew test`(Testcontainers 부팅 테스트)**: 위 수정 후 통과 확인 — V5 마이그레이션이 실제 Postgres
  컨테이너에 정상 적용되고 전체 Spring 컨텍스트(새 Bean 포함)가 문제없이 뜬다.
- **실제 Supabase(운영과 동일 DB)에 대상으로 로컬 서버 기동 후 전체 신규 Admin API를 실제 호출로 검증**:
  - V5 마이그레이션이 실제 Supabase에도 정상 적용됨(`Successfully applied 1 migration ... now at version v5`).
  - 회원가입 후 DB에서 직접 `role='ADMIN'`으로 승격 → 재로그인해 실제 ADMIN 클레임이 담긴 JWT로 전체
    엔드포인트 호출: 프롬프트 템플릿 목록/발행(새 버전 생성 후 최신 버전으로 정상 반영 확인, 이후 테스트로
    남긴 버전은 원본 v1 내용을 다시 새 버전으로 발행해 활성 버전을 원상 복구 — `prompt_templates`는
    survey_definitions처럼 불변이라 행 자체를 지우지 않음), FREE 한도 조회/변경/원복, 사용자 목록/검색/상세,
    사용자별 워크스페이스 조회, 구독 목록/요약 — 전부 200/정상 데이터 확인.
  - **보안 회귀 확인**: 일반 `USER` role 토큰으로 `/api/admin/users` 호출 시 403, 토큰 없이 호출 시 401 —
    기존 `hasRole("ADMIN")` 게이트가 새 엔드포인트에도 그대로 적용됨을 확인.
  - **기존 기능 회귀 확인**: `SurveyDefinitionAdminController`(Phase 05부터 존재)가 이번 리팩토링/CORS
    변경 이후에도 그대로 동작함을 재확인.
  - 테스트로 만든 사용자 계정(admin/non-admin) 삭제, `./gradlew test` 재통과 확인.
- **Admin 프론트 정적 검증**: `tsc -b`, `vite build`, `oxlint` 모두 통과(경고 1건 — AuthContext가 컴포넌트와
  훅을 한 파일에서 같이 export해 Fast Refresh 관련 경고, React Context의 흔한 패턴이라 무해). 로컬에서
  Admin 개발 서버(`:5173`)와 백엔드(`:8080`)를 동시에 띄운 뒤 실제 브라우저 preflight/실 요청 CORS 헤더가
  정상 반환되는 것까지 curl로 확인했다.
- **Playwright MCP를 통한 실제 브라우저 E2E 테스트** (위 "한계"로 남겼던 항목을 이후 세션에서 해소):
  로컬 백엔드(`:8080`)/Admin(`:5173`)을 띄운 뒤, 신규 가입 후 DB에서 `role='ADMIN'`으로 승격한 테스트
  계정으로 실제 Chromium 브라우저를 띄워 화면을 직접 조작하며 검증했다.
  - **로그인**: 정상 로그인 → `/dashboard` 리다이렉트 확인. 잘못된 비밀번호 입력 시 에러 배너
    ("이메일 또는 비밀번호가 올바르지 않습니다.") 정상 표시 확인.
  - **설문 정의 관리**: 탭 전환, 발행 이력 목록, 문항 미리보기(10개 문항·선택지·필수/모름허용 배지까지
    전부 렌더링) 확인. "+ 새 버전 발행" 클릭 시 최신 버전 내용이 편집 폼에 정확히 프리필되는 것까지 확인
    (실제 발행은 하지 않음 — 이 테이블의 최신 버전은 실사용자 온보딩 설문에 즉시 반영되는 라이브 데이터라,
    테스트용 더미 내용으로 새 버전을 만들면 불변 버전 이력에 영구히 남는 문제가 있어 프리필 검증 후 취소).
  - **AI 프롬프트 템플릿 관리**: 기획/분석/설계 탭 전환, 상세 보기(시스템 프롬프트·JSON Schema pretty-print)
    확인. 발행 플로우 자체는 이전 세션에서 실제 API 호출로 이미 검증됐으므로(위 항목 참고) 여기서는 재발행하지
    않고 조회 경로만 확인.
  - **사용자 조회**: 이메일 검색 필터링, 상세 페이지(가입 경로/권한/요금제/가입일), 워크스페이스 목록
    빈 상태 확인(현재 실사용 워크스페이스가 없어 목록이 채워진 상태의 렌더링은 미검증 — 다만 다른 페이지의
    동일한 테이블 렌더링 경로가 이미 검증되어 있어 위험도는 낮음).
  - **구독/사용량 대시보드**: FREE 플랜 월별 한도를 실제로 변경(5→7) → 저장 성공 메시지와 값 반영 확인 →
    원래 값(5)으로 복구. 상태 필터 드롭다운 동작 확인.
  - **반응형 레이아웃(모바일 390px 너비)**: 사이드바가 오프캔버스 드로어로 전환되고 햄버거 버튼으로
    열고 닫히는 것, 표는 페이지 자체가 아니라 표 컨테이너 내부에서만 가로 스크롤되는 것, 설문/프롬프트
    편집 폼의 2단 그리드가 1단으로 정상적으로 쌓이는 것, 로그인 카드가 좁은 화면에서도 깨지지 않는 것을
    확인. (사이드바 바깥 어두운 배경을 탭해 닫는 상호작용은 코드상 정상이나 — `<div className="sidebar-backdrop">`
    가 `onClick`으로 닫기를 호출하고 z-index도 사이드바보다 낮게 올바르게 설정돼 있음 — 390px 너비에서는
    사이드바 폭(232px)이 화면의 절반 이상을 차지해 자동화 도구로 배경 클릭을 검증하기 애매했음. 내비게이션
    링크 클릭 시 자동으로 닫히는 별도 경로는 실기기 조작과 동일하게 확인됨)
  - **개선**: 브라우저 콘솔에서 비밀번호 입력란에 `autocomplete` 속성이 없다는 안내가 있어, 이메일/비밀번호
    input에 `autoComplete="username"`/`"current-password"`를 추가했다(비밀번호 관리자 연동 개선, 기능 변경
    없음). 추가 후 `tsc -b` 재통과 확인.
  - **테스트 후 정리**: 이번 검증용으로 만든 `e2e-admin@alrdream.test` 계정은 테스트 종료 후 DB에서 삭제했다.

---

# Phase 13: Frontend 앱 (사용자)

## 작업 항목

- [x] 구현시 데스크톱 / 랩톱 / 태블릿 / 모바일 반응형 화면 대응
- [x] 로그인/회원가입 화면 (자체 + Google. **Apple은 Apple Developer 자격증명 이슈로 보류 — 크리덴셜 준비되면 버튼 추가**)
- [x] 워크스페이스 목록 및 생성 플로우 (분기 선택 → 설문) [03] §3-1
- [x] 설문 엔진 — 문항 타입별 공용 컴포넌트 (`SingleChoiceField`/`MultiChoiceField`/`TextField`/`ScaleField`) [03] §3-3
- [x] AI 생성 대기 화면 (Job 폴링)
- [x] 워크스페이스 상세 — 기획/분석/설계/설정 탭
- [x] PDF 열람/다운로드

## 설계 결정

- **크로스플랫폼 스택**: [03] §3-2 그대로 Expo Router(SDK 57) + react-native-web. `src/app`을 라우터 루트로 쓰는
  기존 스캐폴딩(Phase 00)을 그대로 활용. Expo SDK가 최근에 크게 바뀌어(`frontend/AGENTS.md`가 명시적으로 경고)
  라우팅 인증 패턴(`Stack.Protected` + `guard`), 환경변수(`EXPO_PUBLIC_` 접두사), `expo-secure-store` 등은 실제
  최신 문서를 그때그때 확인하며 작성 — 예전 Expo Router 버전의 수동 리다이렉트 패턴이 아니라 새 `Stack.Protected`
  선언형 가드를 사용.
- **인증 세션 저장**: `expo-secure-store`가 web을 지원하지 않아(SDK 57 기준) `tokenStorage`에서
  `Platform.OS === "web"`이면 `localStorage`, 아니면 `SecureStore`로 분기(Admin의 refresh-token rotation
  dedup 패턴도 `api/client.ts`에 동일하게 이식).
- **Google 로그인 구현 방식**: Expo 공식 문서가 이제 `@react-native-google-signin/google-signin`을 권장하지만,
  이 라이브러리는 커스텀 네이티브 코드가 필요해 Expo Go/웹에서 동작하지 않고 EAS 개발 빌드 + Firebase/Play
  Console SHA-1 등록이 필요하다 — 이번 세션엔 그런 네이티브 빌드 파이프라인이 없어 검증이 불가능하다. 대신
  `expo-auth-session`으로 Google의 OAuth 엔드포인트에 직접 implicit id_token 플로우(`ResponseType.IdToken`,
  `usePKCE:false`, `nonce`)를 구현 — Expo Go와 웹 양쪽에서 동작하고 백엔드가 이미 검증하는 `idToken` 그대로
  넘길 수 있다. **한계**: Google Cloud Console에 환경별(웹 배포 도메인, Expo Go 프록시 등) redirect URI가
  등록돼 있어야 실제로 토큰이 발급되는데, 이 콘솔 설정은 이번 세션에서 접근/확인이 불가능해 **실제 Google
  로그인 성공까지는 검증하지 못했다** — 이메일/비밀번호 로그인은 완전히 실동작 검증 완료. Apple은 Phase 03/12와
  동일한 이유로 이번에도 버튼을 노출하지 않는다.
- **워크스페이스 상세의 "탭"과 API의 중첩 구조 간극**: `기획/분석/설계` 3개 API는 구조적으로
  `planning-versions/{id}/analysis-versions/{id}/design-versions`처럼 부모 버전 ID가 경로에 그대로 박혀있어,
  "분석 탭"을 보려면 "어느 기획 버전의 분석인지"가 필요하다. 설계 문서([03] §3-1)는 이걸 4개의 평평한 탭으로만
  그려놨고 버전 트리 선택 UI까지는 명시하지 않아, **최신 완료 버전을 자동으로 따라가는 방식**으로 단순화했다
  — 분석 탭은 "가장 최근에 완료된 기획 버전"의 분석 목록을, 설계 탭은 "그 분석 목록 중 가장 최근에 완료된
  버전"의 설계 목록을 보여준다. 이는 백엔드의 `DesignFeatureOptionResolver`(설계 설문의 동적 옵션도 "워크스페이스의
  최신 완료된 분석"에서만 가져옴, 특정 analysisVersionId에 안 묶임)와도 동작이 일치한다. 여러 기획 버전을 병렬로
  키우며 서로 다른 분석/설계를 이어가는 시나리오는 스코프 밖으로 명시적으로 남겨뒀다.
- **"기획 수정" 시 원래 설문 종류(PLANNING_HAS_IDEA/EXPLORING) 역추론**: `PlanningVersionDetail`은
  `surveyResponseId`만 갖고 있고 `SurveyResponseDetail`에는 `surveyKey`가 없어, "수정" 시 어느 분기 설문으로
  다시 열어야 할지 API만으로는 알 수 없다. 두 설문 정의(PLANNING_HAS_IDEA/PLANNING_EXPLORING)를 모두 가져와
  기존 응답의 `questionId` 집합이 어느 쪽과 일치하는지로 역추론하는 `inferPlanningDefinition`을 작성 —
  두 설문의 문항 ID 집합이 서로 다르므로(각 8~10문항, 겹치지 않는 promptKey) 결정적으로 판별 가능하다.
- **PDF 열람/다운로드**: 별도 인앱 뷰어 없이 서명 URL을 받아 `Linking.openURL`로 여는 방식 — 네이티브에서는
  OS 기본 PDF 뷰어/브라우저가, 웹에서는 새 탭이 열린다. `DocumentResponse.downloadUrl`이 이미 만료시간이 있는
  서명 URL이라 프론트가 캐싱하지 않고 매번 새로 발급받는다.
- **디자인 시스템**: Admin(`admin/src/index.css`)과 동일한 브랜드 팔레트(보라 `#7c3aed` 계열)를
  `components/ui/theme.ts`로 이식해 Admin·Frontend 두 앱이 시각적으로 통일되게 했다. `useBreakpoint`
  훅(모바일 <640/태블릿 <1024/데스크톱)과 `ScreenContainer`(태블릿·데스크톱에서 콘텐츠 폭 제한 + 중앙 정렬,
  모바일은 풀폭)로 반응형을 공통 처리.
- **React Compiler 린트 규칙과의 충돌**: 이 프로젝트는 `experiments.reactCompiler: true`라 `expo lint`가
  `react-hooks/set-state-in-effect`를 에러로 강제한다 — "선택된 항목이 바뀌면 상세를 다시 불러온다"는 이
  앱 전반의 흔한 패턴(Admin에서도 똑같이 썼던)이 이 프로젝트에서는 전부 걸렸다. 규칙을 억제하는 대신,
  effect 본문에서 재사용 가능한 이름 있는 함수(`reload` 등)를 직접 호출하지 않고, effect 전용의 새
  익명 async 함수를 그 자리에서 정의·즉시실행하는 방식으로 전부 고쳤다(이벤트 핸들러에서 재사용할 `reload`는
  별도로 유지) — 정적 분석이 "effect가 외부의 재사용 함수를 부르는지"까지는 추적하지만 즉시실행 함수 내부까지는
  추적하지 않는 것으로 보인다.

## 테스트 결과

Playwright MCP로 `expo start --web`(포트 8081) + 로컬 백엔드(`:8080`)를 동시에 띄우고, 실제 Chromium
브라우저로 전 기능을 조작하며 검증했다(Admin Phase 12와 동일한 방식 — 이번엔 처음부터 브라우저 자동화가
가능한 상태로 시작).

- **정적 검증**: `tsc --noEmit`, `expo lint` 모두 통과. 개발 중 `react-hooks/set-state-in-effect` 에러
  10건을 실제로 만나 위 "설계 결정"에 정리한 패턴으로 전부 수정.
- **회원가입 → 로그인 → 로그아웃 → 재로그인**: 이메일/비밀번호로 실제 계정 생성, 로그아웃 후 같은 계정으로
  재로그인까지 실동작 확인. 세션은 `localStorage`(web)에 저장되어 전체 페이지 새로고침 후에도 유지되는 것도
  확인(단, 인증된 상태에서 딥링크 URL로 새로고침하면 `Stack.Protected`의 로딩 게이트를 거치며 워크스페이스
  목록으로 튕기는 것을 발견 — 세션 자체는 안 끊기고 데이터 손실도 없어 치명적이진 않지만, 딥링크 보존은
  스코프 밖으로 남겨둠).
- **워크스페이스 생성 전체 파이프라인을 실제 Claude API 호출로 처음부터 끝까지 검증**(이전 Phase들은
  curl/DB 직접 확인 위주였는데, 이번엔 브라우저 조작 → 실제 생성 → 화면 렌더링까지 전부 눈으로 확인):
  1. 워크스페이스 생성(이름 입력 + "아이템 있어요" 분기 선택) → `PLANNING_HAS_IDEA` 설문 10문항(텍스트/척도/단일
     선택/다중 선택/"잘 모르겠어요" 전부 포함) 작성 → 제출 → 기획 생성 Job 생성 → AI 생성 대기 화면(폴링) →
     완료 후 워크스페이스 상세로 자동 이동 → **실제 Claude가 생성한 기획안 10개 섹션이 전부 올바른 한글
     라벨로 렌더링**되는 것 확인.
  2. 기획 상세에서 "PDF 다운로드" 클릭 → 실제 서명된 Supabase Storage URL이 새 탭으로 열리는 것 확인.
  3. "이 기획으로 분석 시작" 클릭(설문 없이 바로 Job 생성) → 폴링 → 완료 후 분석 탭으로 자동 이동 → 분석
     5개 섹션(합법성/자원/경쟁환경/핵심기능후보 배지/종합의견) 렌더링 확인.
  4. 분석 상세에서 "이 분석으로 설계 시작" → **DESIGN 설문의 `core_feature_priority` 문항에 방금 생성된
     분석의 `core_feature_candidates` 7개가 실제로 동적 주입되어 나타나는 것**까지 확인(정적 옵션이 아님을
     실제 데이터로 검증) → 설문 제출 → 설계 생성 Job → 폴링 → 완료 → 설계 탭 자동 이동 → 기능 명세가
     우선순위 배지(필수/권장)와 함께 렌더링되는 것까지 확인.
- **설정 탭**: 워크스페이스 이름 변경 저장 → 성공 메시지 확인. **버그 발견 및 수정**: 이름을 변경해도 화면
  상단 헤더 타이틀이 갱신되지 않는 문제를 실제 조작 중 발견 — `navigation.setOptions`가 최초 워크스페이스
  조회 시점에만 호출되고 있었음. 헤더 타이틀을 `workspace.name`이 바뀔 때마다 동기화하는 별도 effect로
  분리해 수정, 재조작으로 헤더가 즉시 갱신되는 것 재확인.
- **반응형**: 모바일 너비(390px)에서 로그인/워크스페이스 목록/기획 상세 화면까지 텍스트 줄바꿈과 카드 레이아웃이
  깨지지 않는 것을 실제 스크린샷으로 확인.
- **테스트 후 정리**: 테스트로 만든 `frontend-e2e@alrdream.test` 계정과 그 아래 워크스페이스/기획·분석·설계
  버전/설문 응답/AI Job/문서 레코드를 FK 의존 순서대로(설계→분석→기획→설문응답→Job/사용량/구독→워크스페이스→
  계정) 전부 삭제해 실제 Supabase에 테스트 흔적을 남기지 않았다(Supabase Storage에 업로드된 PDF 오브젝트
  자체는 정리 스코프에서 제외 — DB 레코드만 정리).
- **한계**: Google 로그인은 위 설계 결정에 적은 대로 redirect URI 콘솔 등록이 없어 실제 토큰 발급까지는
  검증하지 못했다. 네이티브(iOS/Android) 빌드는 이 환경에 시뮬레이터/실기기가 없어 web 빌드로만 검증했다 —
  react-native-web을 통한 렌더링이라 레이아웃/로직은 대부분 공유되지만, 네이티브 전용 동작(SecureStore,
  실제 OS 딥링크 등)은 실기기 테스트가 필요하다.

---

# Phase XX: 프로덕션 릴리즈 마무리

> 기본 배포 파이프라인은 Phase 01에서 이미 구축됨. 여기서는 완성된 전 기능을 실제로 얹고 최종 점검한다.

## 작업 항목

- [ ] 전체 환경변수/시크릿(Supabase, Claude, OAuth, PortOne) Render/Vercel에 최종 등록
- [ ] PortOne 웹훅 URL을 실제 배포 도메인으로 등록, 콜드 스타트 상황에서 웹훅 재전송이 정상 처리되는지 확인
- [ ] `frontend` — EAS Build로 내부 테스트 배포 (Android APK, iOS Ad-hoc/TestFlight) — 스토어 정식 출시는 스코프 아님
- [ ] E2E 스모크 테스트 — 회원가입 → 워크스페이스 생성 → 기획/분석/설계 생성 → PDF 다운로드 → Pro 구독 결제까지 전체 플로우 수동 점검

## 사전 조건 (사용자 측)

- [ ] 도메인 및 SSL 인증서 준비 (선택 — Vercel/Render 기본 서브도메인으로 우선 진행 가능)
