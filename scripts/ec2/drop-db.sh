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

echo "Target database: $MYSQL_DATABASE"
read -r -p "Type DROP to delete this database: " confirm

if [[ "$confirm" != "DROP" ]]; then
  echo "Cancelled."
  exit 1
fi

mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p -e "
DROP DATABASE IF EXISTS \`$MYSQL_DATABASE\`;
"
