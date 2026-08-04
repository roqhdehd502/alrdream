# 빌드 컨텍스트는 리포지토리 루트를 기준으로 한다 (database/ 마이그레이션을 함께 복사하기 위함).
# 예: docker build -f backend/Dockerfile .
# Render 배포 시 render.yaml의 dockerContext(".")/dockerfilePath("backend/Dockerfile")가 이를 그대로 반영한다.

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace/backend
COPY backend/ ./
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app/backend

COPY --from=build /workspace/backend/build/libs/*.jar app.jar
# WORKDIR이 /app/backend이므로, application.yml의 `filesystem:../database/migarations` 상대 경로가
# 로컬 개발 환경과 동일하게 /app/database/migarations 를 그대로 가리킨다.
COPY database/migarations /app/database/migarations

# Render 무료 Web Service(0.1 CPU / 512MB) 기준 시작값 — Koyeb과 스펙이 동일해 그대로 재사용. Phase 01에서 실측 후 조정한다.
ENV JAVA_TOOL_OPTIONS="-XX:+UseSerialGC -Xmx288m -XX:MaxMetaspaceSize=160m -XX:MaxDirectMemorySize=32m -XX:ReservedCodeCacheSize=48m"

# Render는 PORT 환경변수(기본 10000)를 주입하며 서버가 이를 리스닝해야 한다.
# server.port: ${PORT:8080} 로 이미 대응되어 있음. EXPOSE는 정보 제공용.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
