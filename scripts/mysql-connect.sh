#!/usr/bin/env bash

set -euo pipefail

CONTAINER_NAME="${MYSQL_CONTAINER_NAME:-todaybread-mysql}"
DATABASE_NAME="${MYSQL_DATABASE:-todaybread}"
MYSQL_USER_NAME="${MYSQL_USER:-todaybread}"
MYSQL_CHARSET="${MYSQL_DEFAULT_CHARSET:-utf8mb4}"

if [[ -n "${MYSQL_PASSWORD:-}" ]]; then
  PASSWORD_ARG=(-p"${MYSQL_PASSWORD}")
else
  PASSWORD_ARG=(-ptodaybread)
fi

exec docker exec -it "${CONTAINER_NAME}" \
  mysql \
  --default-character-set="${MYSQL_CHARSET}" \
  -u"${MYSQL_USER_NAME}" \
  "${PASSWORD_ARG[@]}" \
  -D "${DATABASE_NAME}" \
  "$@"
