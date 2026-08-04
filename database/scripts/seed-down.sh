#!/usr/bin/env bash
# database/seed.sql이 삽입하는 survey_definitions 3종(PLANNING_HAS_IDEA/PLANNING_EXPLORING/DESIGN v1)을 삭제한다.
#
# ⚠️ 개발/스테이징 환경을 초기화(지웠다가 seed.sh로 다시 깨끗하게 삽입)하기 위한 도구다. 운영(production) DB에서는
# 쓰지 않는다 — 여기서 지우는 3개 행은 테스트용 더미 데이터가 아니라 앱이 설문을 보여주기 위해 반드시 있어야 하는
# 실제 운영 데이터다 (지우면 사용자가 워크스페이스를 생성할 수 없다).
# 다만 이미 실사용자 응답(survey_responses)이 해당 정의를 참조 중이면 FK 제약으로 삭제 자체가 실패한다 — 최소한의 안전장치.
#
# 사용법: ./database/scripts/seed-down.sh          확인 프롬프트 후 진행
#         ./database/scripts/seed-down.sh --yes    확인 없이 바로 실행

set -euo pipefail
cd "$(dirname "$0")/../../backend"

if [ ! -f .env ]; then
  echo "backend/.env가 없습니다. backend/.env.example을 복사해 값을 채워주세요." >&2
  exit 1
fi

if [ "${1:-}" != "--yes" ]; then
  read -r -p "seed.sql이 넣은 survey_definitions 3종을 삭제합니다 (개발/스테이징 전용). 계속하려면 yes 입력: " confirm
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
DELETE FROM survey_definitions
WHERE (survey_key, version) IN (
  ('PLANNING_HAS_IDEA', 1),
  ('PLANNING_EXPLORING', 1),
  ('DESIGN', 1)
);
"
