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
| 배포             | Backend → Koyeb, Admin → Vercel, Frontend → EAS(내부/테스트 배포)               |

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

## 배포

[03_design.md §6](./docs/03_design.md), [04_milestone.md Phase 01/14](./docs/04_milestone.md)에 따라 구성한다.

| 대상             | 플랫폼                            | 비고                                                                                   |
| ---------------- | --------------------------------- | -------------------------------------------------------------------------------------- |
| Backend          | [Koyeb](https://www.koyeb.com)    | Docker 배포, 무료 인스턴스는 1시간 무 트래픽 시 스케일-투-제로                         |
| Admin            | [Vercel](https://vercel.com)      | Vite 정적 빌드 배포                                                                    |
| Frontend         | [EAS Build](https://expo.dev/eas) | 내부/테스트 배포 (Android APK, iOS Ad-hoc/TestFlight) — 스토어 정식 출시는 스코프 아님 |
| Database/Storage | [Supabase](https://supabase.com)  | PostgreSQL + Storage                                                                   |

배포 파이프라인(GitHub Actions)은 마일스톤 Phase 01에서 최소 구성으로 먼저 검증한다.
