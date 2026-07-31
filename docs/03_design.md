<!-- 01_planning_and_analysis.md와 02_ai_survey.md를 참고 하여, 프론트엔드 및 백엔드 설계안을 작성합니다. -->
<!-- 아래의 가이드라인을 참고하여 설계합니다. -->

<!-- Frontend -->
<!-- React 기반으로 크로스 플랫폼 (웹, 앱) -->
<!-- + 러닝 커브 및 코드 푸시 대응 -->

<!-- Backend -->
<!-- 1. Spring Boot (3.x, Gradle) -->
<!-- 1-1. Java (21 LTS) -->
<!-- 1-2. Spring MVC (Virtual Threads), @HttpExchange, Springdoc(Swagger) -->
<!-- 1-3. PostgreSQL, Spring Data JPA (기본적인 CRUD) + Querydsl (복잡한 동적 쿼리 및 통계), Flyway -->
<!-- 1-4. Redis (Lettuce / Redisson) -->
<!-- 1-5. Spring Security 6.x (JWT + OAuth2) -->
<!-- 1-6. JUnit 5, AssertJ, Testcontainers -->
<!-- 1-7. Gradle, Docker, GitHub Actions -->

<!-- Backend Architecture -->
<!--
```
src/main/java/com/example/demo
├── global              # 공통 설정 (Security, Exception, Config, Util)
│   ├── config
│   ├── error
│   └── security
├── domain              # 도메인별 모듈 (Domain-Driven Structure)
│   ├── member
│   │   ├── api         # Controller, Request/Response DTO
│   │   ├── application # Service, UseCase
│   │   ├── domain      # Entity, VO, Repository Interface
│   │   └── infrastructure # Repository Implementation, External Client
│   └── order
└── infrastructure      # 외부에 의존하는 공통 인프라 (Redis, S3, Mail 등)
```
-->
