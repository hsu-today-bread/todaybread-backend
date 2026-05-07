#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SQL_FILE="${1:-${SCRIPT_DIR}/test-data.sql}"

if [[ ! -f "${SQL_FILE}" ]]; then
  echo "SQL file not found: ${SQL_FILE}" >&2
  exit 1
fi

UPLOAD_DIR_PATH="${UPLOAD_DIR:-${REPO_ROOT}/uploads}"
if [[ "${UPLOAD_DIR_PATH}" != /* ]]; then
  UPLOAD_DIR_PATH="${REPO_ROOT}/${UPLOAD_DIR_PATH}"
fi

SEED_IMAGES_DIR="${SCRIPT_DIR}/seed-images"

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

# ── SVG 플레이스홀더 생성 함수 ──

write_seed_svg() {
  local filename="$1"
  local title="$2"
  local subtitle="$3"
  local background="$4"
  local accent="$5"
  local target="${UPLOAD_DIR_PATH}/${filename}"

  cat > "${target}" <<SVG
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

# ── 이미지 생성/복사 ──

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

copy_or_write_seed_image() {
  local target_file="$1"
  local source_key="$2"
  local title="$3"
  local subtitle="$4"
  local source_path

  if source_path="$(find_seed_image "${source_key}")"; then
    cp "${source_path}" "${UPLOAD_DIR_PATH}/${target_file}"
  else
    write_seed_svg "${target_file}" "${title}" "${subtitle}" "#6f5b4a" "#e7b76d"
  fi
}

create_seed_images() {
  mkdir -p "${UPLOAD_DIR_PATH}"

  echo "(Upload directory prepared. Seed image files will be copied after SQL execution.)"
}

# ── 매장별 이미지 파일 생성 (SQL 실행 후) ──

create_per_store_images() {
  echo "Creating per-store image files..."

  local filenames
  filenames=$(docker exec "${CONTAINER_NAME}" \
    mysql --batch --skip-column-names \
    -h"${MYSQL_HOST}" \
    -u"${MYSQL_USER_NAME}" \
    "${PASSWORD_ARG[@]}" \
    -D "${DATABASE_NAME}" \
    -e "SELECT si.stored_filename, SUBSTRING_INDEX(si.original_filename, '.', 1) AS source_key FROM store_image si JOIN store s ON si.store_id = s.id JOIN users u ON s.user_id = u.id WHERE u.email LIKE 'demo-boss%@todaybread.com';" 2>/dev/null || true)

  if [[ -z "${filenames}" ]]; then
    echo "Warning: Could not fetch store image filenames from DB. Skipping per-store images."
    return
  fi

  while IFS=$'\t' read -r target_file source_key; do
    copy_or_write_seed_image "${target_file}" "${source_key}" "Store Image" "store image"
  done <<< "${filenames}"

  echo "Per-store image files created."
}

# ── 빵별 이미지 파일 생성 (SQL 실행 후) ──

create_per_bread_images() {
  echo "Creating per-bread image files..."

  # DB에서 bread_image 테이블의 stored_filename 목록을 가져옴
  local filenames
  filenames=$(docker exec "${CONTAINER_NAME}" \
    mysql --batch --skip-column-names \
    -h"${MYSQL_HOST}" \
    -u"${MYSQL_USER_NAME}" \
    "${PASSWORD_ARG[@]}" \
    -D "${DATABASE_NAME}" \
    -e "SELECT bi.stored_filename, SUBSTRING_INDEX(bi.original_filename, '.', 1) AS source_key FROM bread_image bi JOIN bread b ON bi.bread_id = b.id JOIN store s ON b.store_id = s.id JOIN users u ON s.user_id = u.id WHERE u.email LIKE 'demo-boss%@todaybread.com';" 2>/dev/null || true)

  if [[ -z "${filenames}" ]]; then
    echo "Warning: Could not fetch bread image filenames from DB. Skipping per-bread images."
    return
  fi

  while IFS=$'\t' read -r target_file source_key; do
    copy_or_write_seed_image "${target_file}" "${source_key}" "Bread Image" "bread image"
  done <<< "${filenames}"

  echo "Per-bread image files created."
}

# ── 리뷰별 이미지 파일 생성 (SQL 실행 후) ──

create_per_review_images() {
  echo "Creating per-review image files..."

  local filenames
  filenames=$(docker exec "${CONTAINER_NAME}" \
    mysql --batch --skip-column-names \
    -h"${MYSQL_HOST}" \
    -u"${MYSQL_USER_NAME}" \
    "${PASSWORD_ARG[@]}" \
    -D "${DATABASE_NAME}" \
    -e "SELECT ri.stored_filename, SUBSTRING_INDEX(ri.original_filename, '.', 1) AS source_key FROM review_image ri JOIN review r ON ri.review_id = r.id JOIN users u ON r.user_id = u.id WHERE u.email = 'demo-user@todaybread.com';" 2>/dev/null || true)

  if [[ -z "${filenames}" ]]; then
    echo "Warning: Could not fetch review image filenames from DB. Skipping per-review images."
    return
  fi

  while IFS=$'\t' read -r target_file source_key; do
    copy_or_write_seed_image "${target_file}" "${source_key}" "Review Image" "review image"
  done <<< "${filenames}"

  echo "Per-review image files created."
}

# ── 메인 실행 ──

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
    -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '${DATABASE_NAME}' AND table_name IN ('users', 'store', 'favourite_store', 'orders', 'order_item', 'review', 'review_image', 'payment');"
)"

if [[ "${SCHEMA_READY}" != "8" ]]; then
  cat >&2 <<'EOF'
Database schema is not initialized yet.
Run the Spring Boot app once so Flyway can create the tables, then run:
  ./scripts/test-data.sh
EOF
  exit 1
fi

create_seed_images
echo "Base seed images written to ${UPLOAD_DIR_PATH}"

docker exec -i "${CONTAINER_NAME}" \
  mysql \
  --default-character-set="${MYSQL_CHARSET}" \
  -h"${MYSQL_HOST}" \
  -u"${MYSQL_USER_NAME}" \
  "${PASSWORD_ARG[@]}" \
  -D "${DATABASE_NAME}" \
  < "${SQL_FILE}"

create_per_store_images
create_per_bread_images
create_per_review_images

cat <<'EOF'
Test data applied.

Token note
- Access/refresh tokens are not inserted by this seed script.
- Run /api/user/login with a sample account; the app will issue tokens and save refresh_token.

Sample accounts
- demo-user@todaybread.com / todaybread123
- demo-boss1@todaybread.com ~ demo-boss100@todaybread.com / todaybread123

Recommended nearby query
- Hansung Univ: lat=37.5826000, lng=127.0106000, radius=3
- Hansung Univ: lat=37.5826000, lng=127.0106000, radius=5
- Hansung Univ: lat=37.5826000, lng=127.0106000, radius=10

Image replacement
- Place real images in scripts/seed-images/ with names like:
  seed_store_01.jpg, seed_bread_01.jpg, seed_review_01.jpg, etc.
- Re-run this script to replace SVG placeholders.
EOF
