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

if [[ -n "${MYSQL_PASSWORD:-}" ]]; then
  PASSWORD_ARG=(-p"${MYSQL_PASSWORD}")
else
  PASSWORD_ARG=(-ptodaybread)
fi

echo "Applying development test data to container '${CONTAINER_NAME}' / database '${DATABASE_NAME}'..."

docker exec -i "${CONTAINER_NAME}" \
  mysql \
  --default-character-set="${MYSQL_CHARSET}" \
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
