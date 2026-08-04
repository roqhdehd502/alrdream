# 알려드림 (alrdream)

사용자의 사업 아이템을 AI 기반으로 기획 → 분석 → 설계까지 자동 생성해주는 워크스페이스형 서비스.

기획/설문/설계/마일스톤 등 프로젝트 전반의 의사결정 기록은 [`docs/`](./docs)에 있다.

- [01. 기획 및 분석](./docs/01_planning_and_analysis.md)
- [02. AI 설문 설계](./docs/02_ai_survey.md)
- [03. 설계](./docs/03_design.md)
- [04. 마일스톤](./docs/04_milestone.md)

---

## 구성

모노레포 구조로, 앱별 디렉토리가 독립적으로 빌드/배포된다.

```
alrdream/
├── admin/       # 운영자용 웹 콘솔 (React + Vite)
├── backend/     # API 서버 (Spring Boot)
├── frontend/    # 사용자용 앱 (Expo — 웹/iOS/Android)
├── database/    # Flyway 마이그레이션 + 시드 데이터
└── docs/        # 기획/설계/마일스톤 문서
```

## 기술 스택

| 영역             | 스택                                                                            |
| ---------------- | ------------------------------------------------------------------------------- |
| Admin            | React, Vite, TypeScript                                                         |
| Frontend         | Expo (React Native), Expo Router, react-native-web                              |
| Backend          | Spring Boot 4.x, Java 21, Spring Data JPA + Querydsl, Flyway, Spring Data Redis |
| Database/Storage | Supabase (PostgreSQL + S3 호환 Storage)                                         |
| AI               | Anthropic Claude API                                                            |
| 결제             | 포트원(PortOne) V2, PG사: 토스페이먼츠                                          |
| 배포             | Backend → Render, Admin → Vercel, Frontend → EAS(내부/테스트 배포)              |

자세한 배경과 근거는 [03_design.md](./docs/03_design.md) 참고.

---

## 개발 환경 셋업

### 사전 준비물

- Java 21
- Node.js 20+
- Docker (로컬 Redis, `./gradlew test`의 Testcontainers 실행에 필요)
- Supabase 프로젝트 (PostgreSQL + Storage)
- Anthropic Claude API Key
- 포트원(PortOne) 가맹점 계정

### 1. 로컬 인프라 (Redis)

```bash
docker compose up -d
```

### 2. Backend

```bash
cd backend
cp .env.example .env   # 값 채우기 (가이드: .env.example 주석 참고)
./gradlew bootRun       # http://localhost:8080
```

`.env`는 `spring.config.import`로 자동 로드되며 git에 커밋되지 않는다. Swagger UI는 `/swagger-ui.html`.

### 3. Frontend (Expo)

```bash
cd frontend
npm install
npm run web       # 웹으로 실행
npm run ios       # iOS 시뮬레이터
npm run android   # Android 에뮬레이터
```

### 4. Admin

```bash
cd admin
npm install
npm run dev        # http://localhost:5173
```

---

## 데이터베이스 마이그레이션 / 시드 데이터

`database/migarations/`는 Flyway 마이그레이션(`V<n>__설명.sql`), `database/seed.sql`은 앱 동작에 필요한 초기 데이터([02] §5 설문 정의 3종)를 담고 있다. 둘 다 `backend/.env`의 Supabase 접속정보를 사용한다.

일반적인 로컬 개발은 `./gradlew bootRun`이 부팅 시 자동으로 마이그레이션을 적용해주므로 아래 스크립트를 따로 쓸 필요가 없다. 앱을 띄우지 않고 DB만 조작하고 싶을 때(스테이징 점검, CI 등) 사용한다.

```bash
# 마이그레이션 — backend/build.gradle의 Flyway Gradle 플러그인을 사용, 앱 부팅과 동일한 flyway_schema_history를 남긴다
./database/scripts/migrate.sh          # 모든 미적용 마이그레이션 실행
./database/scripts/migrate.sh info     # 현재 적용 상태만 조회
./database/scripts/migrate.sh 2        # V2까지만 실행 (이미 적용된 버전은 자동 스킵)
# Flyway는 버전 순서를 보장하는 도구라 "파일 하나만 단독 실행"은 지원하지 않는다 — 위 target 방식이 안전한 대안이다.

# 시드 데이터 삽입 — ON CONFLICT DO NOTHING이라 여러 번 실행해도 안전
./database/scripts/seed.sh

# 시드 데이터 삭제 — 개발/스테이징 초기화 전용. seed.sh가 넣는 3개 행은 테스트용 더미가 아니라
# 앱이 설문을 보여주기 위해 반드시 있어야 하는 데이터라 운영 DB에서는 실행하지 않는다.
./database/scripts/seed-down.sh          # 확인 프롬프트 후 삭제
./database/scripts/seed-down.sh --yes    # 확인 없이 바로 삭제
```

---

## 배포

[03_design.md §6](./docs/03_design.md), [04_milestone.md Phase 01/14](./docs/04_milestone.md)에 따라 구성한다.

| 대상             | 플랫폼                            | 비고                                                                                   |
| ---------------- | --------------------------------- | -------------------------------------------------------------------------------------- |
| Backend          | [Render](https://render.com)      | Docker 배포, 무료 Web Service는 15분 무 트래픽 시 스핀다운. `render.yaml`(Blueprint)로 정의, GitHub App이 push 시 자동 배포 (GitHub Actions 불필요) |
| Admin            | [Vercel](https://vercel.com)      | Vite 정적 빌드 배포                                                                    |
| Frontend         | [EAS Build](https://expo.dev/eas) | 내부/테스트 배포 (Android APK, iOS Ad-hoc/TestFlight) — 스토어 정식 출시는 스코프 아님 |
| Database/Storage | [Supabase](https://supabase.com)  | PostgreSQL + Storage                                                                   |

배포 파이프라인은 마일스톤 Phase 01에서 최소 구성으로 먼저 검증한다.
