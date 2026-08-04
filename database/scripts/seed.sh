#!/usr/bin/env bash
# database/seed.sql을 실행해 초기 survey_definitions 3종을 삽입한다.
# seed.sql이 ON CONFLICT (survey_key, version) DO NOTHING으로 작성되어 있어 여러 번 실행해도 안전하다.

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

PGPASSWORD="$SUPABASE_DB_PASSWORD" psql "$CONN_URL" -v ON_ERROR_STOP=1 -f ../database/seed.sql
