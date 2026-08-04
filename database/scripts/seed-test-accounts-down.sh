#!/usr/bin/env bash
# database/seed-test-accounts.sql이 넣는 role별 테스트 계정(admin@alrdream.test, user@alrdream.test)을 삭제한다.
#
# 사용법: ./database/scripts/seed-test-accounts-down.sh          확인 프롬프트 후 진행
#         ./database/scripts/seed-test-accounts-down.sh --yes    확인 없이 바로 실행

set -euo pipefail
cd "$(dirname "$0")/../../backend"

if [ ! -f .env ]; then
  echo "backend/.env가 없습니다. backend/.env.example을 복사해 값을 채워주세요." >&2
  exit 1
fi

if [ "${1:-}" != "--yes" ]; then
  read -r -p "테스트 계정(admin@alrdream.test, user@alrdream.test)을 삭제합니다. 계속하려면 yes 입력: " confirm
  if [ "$confirm" != "yes" ]; then
    echo "취소되었습니다."
    exit 1
  fi
fi

set -a
source .env
set +a

DB_HOST_PORT_NAME="${SUPABASE_DB_URL#jdbc:postgresql://}"
CONN_URL="postgresql://${SUPABASE_DB_USERNAME}@${DB_HOST_PORT_NAME}"

PGPASSWORD="$SUPABASE_DB_PASSWORD" psql "$CONN_URL" -v ON_ERROR_STOP=1 -c "
DELETE FROM users
WHERE email IN ('admin@alrdream.test', 'user@alrdream.test');
"
