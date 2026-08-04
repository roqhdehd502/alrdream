#!/usr/bin/env bash
# database/migarations/*.sql 을 Flyway로 실행한다.
# backend/build.gradle에 등록된 Flyway Gradle 플러그인을 사용하므로, 앱을 통째로 기동하지 않고도
# Spring Boot가 부팅 시 실행하는 것과 동일한 flyway_schema_history 이력을 남긴다.
#
# 사용법:
#   ./database/scripts/migrate.sh          모든 미적용 마이그레이션 실행
#   ./database/scripts/migrate.sh info     현재 적용 상태만 조회 (아무것도 실행하지 않음)
#   ./database/scripts/migrate.sh 2        V2까지만 실행 (이미 적용된 버전은 자동으로 건너뜀, 롤백 아님)
#
# 주의: Flyway는 버전 순서를 보장하는 도구라 "이 파일 하나만 단독/역순 실행"은 지원하지 않는다.
# "특정 버전까지 실행"(target)이 "특정 파일만 실행"의 안전한 대응 방식이다.

set -euo pipefail
cd "$(dirname "$0")/../../backend"

if [ ! -f .env ]; then
  echo "backend/.env가 없습니다. backend/.env.example을 복사해 값을 채워주세요." >&2
  exit 1
fi

set -a
source .env
set +a

export FLYWAY_URL="$SUPABASE_DB_URL"
export FLYWAY_USER="$SUPABASE_DB_USERNAME"
export FLYWAY_PASSWORD="$SUPABASE_DB_PASSWORD"

case "${1:-}" in
  info)
    ./gradlew -q flywayInfo
    ;;
  "")
    ./gradlew -q flywayMigrate
    ;;
  *[!0-9]*)
    echo "버전은 숫자만 가능합니다 (예: 2 → V2__xxx.sql). 상태 조회는 'info'를 사용하세요." >&2
    exit 1
    ;;
  *)
    FLYWAY_TARGET="$1" ./gradlew -q flywayMigrate
    ;;
esac
