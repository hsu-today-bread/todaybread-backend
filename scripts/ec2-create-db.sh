#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-/home/ubuntu/todaybread/secrets/.env.ec2}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Env file not found: ${ENV_FILE}" >&2
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p -e "
CREATE DATABASE IF NOT EXISTS \`$MYSQL_DATABASE\`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
"
