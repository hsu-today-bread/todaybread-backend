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

required_vars=(
  MYSQL_HOST
  MYSQL_PORT
  MYSQL_DATABASE
  MYSQL_USER
  MYSQL_PASSWORD
  S3_BUCKET
  AWS_REGION
)

for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    echo "Required env is missing: ${var_name}" >&2
    exit 1
  fi
done

if ! command -v mysql >/dev/null 2>&1; then
  echo "mysql client is not installed. Install it with: sudo apt install -y mysql-client" >&2
  exit 1
fi

if ! command -v aws >/dev/null 2>&1; then
  echo "aws CLI is not installed." >&2
  exit 1
fi

cat <<EOF
This will reset demo storage.
- RDS database: ${MYSQL_HOST}/${MYSQL_DATABASE}
- S3 bucket: s3://${S3_BUCKET}/

The RDS database will be dropped.
All objects in the S3 bucket will be deleted.
EOF

read -r -p "Type RESET DEMO to continue: " confirm

if [[ "${confirm}" != "RESET DEMO" ]]; then
  echo "Cancelled."
  exit 1
fi

MYSQL_CHARSET="${MYSQL_DEFAULT_CHARSET:-utf8mb4}"
MYSQL_DEFAULTS_FILE="$(mktemp)"
trap 'rm -f "${MYSQL_DEFAULTS_FILE}"' EXIT

chmod 600 "${MYSQL_DEFAULTS_FILE}"
{
  echo "[client]"
  echo "host=${MYSQL_HOST}"
  echo "port=${MYSQL_PORT}"
  echo "user=${MYSQL_USER}"
  echo "password=${MYSQL_PASSWORD}"
  echo "default-character-set=${MYSQL_CHARSET}"
} > "${MYSQL_DEFAULTS_FILE}"

echo "Dropping database..."
mysql --defaults-extra-file="${MYSQL_DEFAULTS_FILE}" -e "
DROP DATABASE IF EXISTS \`${MYSQL_DATABASE}\`;
"

echo "Clearing S3 bucket..."
aws s3 rm \
  "s3://${S3_BUCKET}/" \
  --recursive \
  --region "${AWS_REGION}"

cat <<EOF
Demo storage reset complete.

Next steps:
1. Run ./scripts/ec2-create-db.sh to create the database.
2. Start the Spring Boot app once so Flyway creates tables.
3. Run ./scripts/ec2-test-data.sh to insert seed data and sync seed images.
EOF
