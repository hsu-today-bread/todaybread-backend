#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_FILE="${1:-${SCRIPT_DIR}/test-data.sql}"

if [[ ! -f "${SQL_FILE}" ]]; then
  echo "SQL file not found: ${SQL_FILE}" >&2
  exit 1
fi

CONTAINER_NAME="${MYSQL_CONTAINER_NAME:-todaybread-mysql}"
DATABASE_NAME="${MYSQL_DATABASE:-todaybread}"
MYSQL_USER_NAME="${MYSQL_USER:-todaybread}"
MYSQL_CHARSET="${MYSQL_DEFAULT_CHARSET:-utf8mb4}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MAX_ATTEMPTS="${MYSQL_WAIT_ATTEMPTS:-30}"

if [[ -n "${MYSQL_PASSWORD:-}" ]]; then
  PASSWORD_ARG=(-p"${MYSQL_PASSWORD}")
else
  PASSWORD_ARG=(-ptodaybread)
fi

echo "Applying development test data to container '${CONTAINER_NAME}' / database '${DATABASE_NAME}'..."

for ((attempt=1; attempt<=MAX_ATTEMPTS; attempt++)); do
  if docker exec "${CONTAINER_NAME}" \
    mysqladmin \
    -h"${MYSQL_HOST}" \
    -u"${MYSQL_USER_NAME}" \
    "${PASSWORD_ARG[@]}" \
    ping >/dev/null 2>&1; then
    break
  fi

  if [[ "${attempt}" -eq "${MAX_ATTEMPTS}" ]]; then
    echo "MySQL is not ready in container '${CONTAINER_NAME}'." >&2
    echo "Check 'docker compose ps' and 'docker logs ${CONTAINER_NAME}'." >&2
    exit 1
  fi

  sleep 1
done

SCHEMA_READY="$(
  docker exec "${CONTAINER_NAME}" \
    mysql \
    --batch --skip-column-names \
    -h"${MYSQL_HOST}" \
    -u"${MYSQL_USER_NAME}" \
    "${PASSWORD_ARG[@]}" \
    -D "${DATABASE_NAME}" \
    -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '${DATABASE_NAME}' AND table_name IN ('users', 'store', 'favourite_store');"
)"

if [[ "${SCHEMA_READY}" != "3" ]]; then
  cat >&2 <<'EOF'
Database schema is not initialized yet.
Run the Spring Boot app once so Flyway can create the tables, then run:
  ./scripts/test-data.sh
EOF
  exit 1
fi

docker exec -i "${CONTAINER_NAME}" \
  mysql \
  --default-character-set="${MYSQL_CHARSET}" \
  -h"${MYSQL_HOST}" \
  -u"${MYSQL_USER_NAME}" \
  "${PASSWORD_ARG[@]}" \
  -D "${DATABASE_NAME}" \
  < "${SQL_FILE}"

cat <<'EOF'
Test data applied.

Sample accounts
- demo-user@todaybread.local / todaybread123
- demo-boss-gangnam@todaybread.local / todaybread123
- demo-boss-seolleung@todaybread.local / todaybread123

Recommended nearby query
- lat=37.4980950
- lng=127.0276100
- radius=3
EOF
