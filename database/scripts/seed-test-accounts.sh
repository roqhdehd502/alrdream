#!/usr/bin/env bash
# database/seed-test-accounts.sql을 실행해 role(USER/ADMIN)별 로그인 테스트 계정을 삽입한다.
# ON CONFLICT (email) DO NOTHING으로 작성되어 있어 여러 번 실행해도 안전하다.
#
# ⚠️ 개발/스테이징 전용. 운영(production) DB에서는 절대 실행하지 않는다

set -euo pipefail
cd "$(dirname "$0")/../../backend"

if [ ! -f .env ]; then
  echo "backend/.env가 없습니다. backend/.env.example을 복사해 값을 채워주세요." >&2
  exit 1
fi

set -a
source .env
set +a

DB_HOST_PORT_NAME="${SUPABASE_DB_URL#jdbc:postgresql://}"
CONN_URL="postgresql://${SUPABASE_DB_USERNAME}@${DB_HOST_PORT_NAME}"

PGPASSWORD="$SUPABASE_DB_PASSWORD" psql "$CONN_URL" -v ON_ERROR_STOP=1 -f ../database/seed-test-accounts.sql
