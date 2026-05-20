#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${ENV_FILE:-/home/ubuntu/todaybread/secrets/.env.ec2}"
SQL_FILE="${1:-${SCRIPT_DIR}/test-data.sql}"
SEED_IMAGES_DIR="${SCRIPT_DIR}/seed-images"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Env file not found: ${ENV_FILE}" >&2
  exit 1
fi

if [[ ! -f "${SQL_FILE}" ]]; then
  echo "SQL file not found: ${SQL_FILE}" >&2
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

MYSQL_CHARSET="${MYSQL_DEFAULT_CHARSET:-utf8mb4}"
MYSQL_DEFAULTS_FILE="$(mktemp)"
WORK_DIR="$(mktemp -d)"
trap 'rm -f "${MYSQL_DEFAULTS_FILE}"; rm -rf "${WORK_DIR}"' EXIT

chmod 600 "${MYSQL_DEFAULTS_FILE}"
{
  echo "[client]"
  echo "host=${MYSQL_HOST}"
  echo "port=${MYSQL_PORT}"
  echo "user=${MYSQL_USER}"
  echo "password=${MYSQL_PASSWORD}"
  echo "database=${MYSQL_DATABASE}"
  echo "default-character-set=${MYSQL_CHARSET}"
} > "${MYSQL_DEFAULTS_FILE}"

schema_ready="$(
  mysql \
    --defaults-extra-file="${MYSQL_DEFAULTS_FILE}" \
    --batch \
    --skip-column-names \
    -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '${MYSQL_DATABASE}' AND table_name IN ('users', 'interest_area', 'store', 'favourite_store', 'orders', 'order_item', 'review', 'review_image', 'payment');"
)"

if [[ "${schema_ready}" != "9" ]]; then
  cat >&2 <<'EOF'
Database schema is not initialized yet.
Run the Spring Boot app once so Flyway can create the tables, then run this script again.
EOF
  exit 1
fi

write_seed_svg() {
  local target_path="$1"
  local title="$2"
  local subtitle="$3"
  local background="$4"
  local accent="$5"

  cat > "${target_path}" <<SVG
<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="800" viewBox="0 0 1200 800" role="img" aria-label="${title}">
  <rect width="1200" height="800" fill="${background}"/>
  <circle cx="1010" cy="150" r="210" fill="${accent}" opacity="0.22"/>
  <circle cx="190" cy="690" r="260" fill="#ffffff" opacity="0.18"/>
  <rect x="86" y="92" width="1028" height="616" rx="34" fill="#ffffff" opacity="0.13"/>
  <text x="110" y="370" font-family="Arial, Helvetica, sans-serif" font-size="74" font-weight="700" fill="#ffffff">${title}</text>
  <text x="114" y="444" font-family="Arial, Helvetica, sans-serif" font-size="34" font-weight="500" fill="#ffffff" opacity="0.86">${subtitle}</text>
  <text x="114" y="640" font-family="Arial, Helvetica, sans-serif" font-size="28" font-weight="700" fill="#ffffff" opacity="0.72">TODAYBREAD SEED IMAGE</text>
</svg>
SVG
}

find_seed_image() {
  local source_key="$1"

  for ext in jpg jpeg png webp svg; do
    if [[ -f "${SEED_IMAGES_DIR}/${source_key}.${ext}" ]]; then
      echo "${SEED_IMAGES_DIR}/${source_key}.${ext}"
      return 0
    fi
  done

  return 1
}

content_type_for() {
  local filename="$1"

  case "${filename##*.}" in
    jpg|jpeg) echo "image/jpeg" ;;
    png) echo "image/png" ;;
    webp) echo "image/webp" ;;
    svg) echo "image/svg+xml" ;;
    *) echo "application/octet-stream" ;;
  esac
}

prepare_seed_image() {
  local object_key="$1"
  local source_key="$2"
  local title="$3"
  local subtitle="$4"
  local target_path="${WORK_DIR}/${object_key}"
  local source_path

  mkdir -p "$(dirname "${target_path}")"

  if source_path="$(find_seed_image "${source_key}")"; then
    cp "${source_path}" "${target_path}"
    content_type_for "${source_path}"
  else
    write_seed_svg "${target_path}" "${title}" "${subtitle}" "#6f5b4a" "#e7b76d"
    echo "image/svg+xml"
  fi
}

echo "Applying seed SQL to ${MYSQL_HOST}/${MYSQL_DATABASE}..."
mysql \
  --defaults-extra-file="${MYSQL_DEFAULTS_FILE}" \
  --default-character-set="${MYSQL_CHARSET}" \
  < "${SQL_FILE}"

image_rows="$(
  mysql \
    --defaults-extra-file="${MYSQL_DEFAULTS_FILE}" \
    --batch \
    --skip-column-names \
    -e "
      SELECT stored_filename, SUBSTRING_INDEX(original_filename, '.', 1), 'Store Image', 'store image'
      FROM store_image
      WHERE stored_filename LIKE 'seed_store_%'
      UNION ALL
      SELECT stored_filename, SUBSTRING_INDEX(original_filename, '.', 1), 'Bread Image', 'bread image'
      FROM bread_image
      WHERE stored_filename LIKE 'seed_bread_%'
      UNION ALL
      SELECT stored_filename, SUBSTRING_INDEX(original_filename, '.', 1), 'Review Image', 'review image'
      FROM review_image
      WHERE stored_filename LIKE 'seed_review_%';
    "
)"

if [[ -z "${image_rows}" ]]; then
  echo "No seed image rows found after seed SQL." >&2
  exit 1
fi

uploaded_count=0

while IFS=$'\t' read -r object_key source_key title subtitle; do
  if [[ -z "${object_key:-}" ]]; then
    continue
  fi

  target_path="${WORK_DIR}/${object_key}"
  content_type="$(prepare_seed_image "${object_key}" "${source_key}" "${title}" "${subtitle}")"

  aws s3 cp \
    "${target_path}" \
    "s3://${S3_BUCKET}/${object_key}" \
    --region "${AWS_REGION}" \
    --content-type "${content_type}" \
    --cache-control "public, max-age=31536000" \
    --only-show-errors

  uploaded_count=$((uploaded_count + 1))
done <<< "${image_rows}"

cat <<EOF
EC2 test data applied.
- RDS: ${MYSQL_HOST}/${MYSQL_DATABASE}
- S3: s3://${S3_BUCKET}
- Uploaded seed images: ${uploaded_count}

Sample accounts:
- demo-user01@todaybread.com ~ demo-user20@todaybread.com / todaybread123
- demo-boss001@todaybread.com ~ demo-boss120@todaybread.com / todaybread123
EOF
