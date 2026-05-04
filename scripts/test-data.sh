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

create_seed_images() {
  mkdir -p "${UPLOAD_DIR_PATH}"

  # 매장 이미지 10장
  local store_colors=(
    "#99624f:#f2b279" "#5f7161:#d1b36a" "#4e6f87:#f0c36d" "#715a83:#e8b56a" "#7a6651:#a7c882"
    "#8b5e3c:#f4d186" "#5a6b4f:#c8e6a0" "#6d4c7a:#d4a5e8" "#4a6e8a:#a8d4f0" "#8c6b4a:#e8c87a"
  )

  for i in $(seq 1 10); do
    local padded
    padded=$(printf "%02d" "$i")
    local base_svg="seed_store_${padded}.svg"

    # scripts/seed-images/ 에 실제 이미지가 있으면 그걸 복사
    local found_real=""
    for ext in jpg jpeg png webp; do
      if [[ -f "${SEED_IMAGES_DIR}/seed_store_${padded}.${ext}" ]]; then
        cp "${SEED_IMAGES_DIR}/seed_store_${padded}.${ext}" "${UPLOAD_DIR_PATH}/${base_svg}"
        found_real="yes"
        break
      fi
    done

    if [[ -z "${found_real}" ]]; then
      IFS=':' read -r bg accent <<< "${store_colors[$((i-1))]}"
      write_seed_svg "${base_svg}" "Store ${padded}" "store image" "${bg}" "${accent}"
    fi
  done

  # 빵 이미지 10장 (기본 원본)
  local bread_colors=(
    "#b97745:#f4d186" "#698353:#f0c56a" "#7b5547:#e8ad65" "#876057:#f3cfa8" "#5a3d38:#d8895b"
    "#b47a3f:#f1c96a" "#8b684d:#e4bd76" "#73624e:#d2ad76" "#6d6577:#cbb6db" "#5b5145:#c2a47a"
  )

  for i in $(seq 1 10); do
    local padded
    padded=$(printf "%02d" "$i")
    local base_svg="seed_bread_${padded}.svg"

    local found_real=""
    for ext in jpg jpeg png webp; do
      if [[ -f "${SEED_IMAGES_DIR}/seed_bread_${padded}.${ext}" ]]; then
        found_real="yes"
        break
      fi
    done

    if [[ -n "${found_real}" ]]; then
      # 실제 이미지가 있으면 — SQL에서 bread_id별로 고유 파일명을 만들므로
      # 여기서는 원본만 복사해두고, 아래에서 bread별 파일을 심볼릭 링크 또는 복사
      cp "${SEED_IMAGES_DIR}/seed_bread_${padded}.${ext}" "${UPLOAD_DIR_PATH}/${base_svg}"
    else
      IFS=':' read -r bg accent <<< "${bread_colors[$((i-1))]}"
      write_seed_svg "${base_svg}" "Bread ${padded}" "bread image" "${bg}" "${accent}"
    fi
  done

  # SQL에서 bread_image.stored_filename = seed_bread_XX_{bread_id}.svg 형태로 생성하므로
  # 해당 파일들도 만들어야 합니다. DB에서 bread_id를 알아야 하므로 SQL 실행 후에 처리합니다.
  echo "(Base seed images created. Per-bread images will be linked after SQL execution.)"
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
    -e "SELECT bi.stored_filename, CONCAT('seed_bread_', LPAD(((ROW_NUMBER() OVER (ORDER BY bi.id) - 1) % 10) + 1, 2, '0'), '.svg') AS source_name FROM bread_image bi JOIN bread b ON bi.bread_id = b.id JOIN store s ON b.store_id = s.id WHERE s.user_id IN (SELECT id FROM users WHERE email LIKE 'demo-boss%@todaybread.com');" 2>/dev/null || true)

  if [[ -z "${filenames}" ]]; then
    echo "Warning: Could not fetch bread image filenames from DB. Skipping per-bread images."
    return
  fi

  while IFS=$'\t' read -r target_file source_file; do
    if [[ -f "${UPLOAD_DIR_PATH}/${source_file}" && ! -f "${UPLOAD_DIR_PATH}/${target_file}" ]]; then
      cp "${UPLOAD_DIR_PATH}/${source_file}" "${UPLOAD_DIR_PATH}/${target_file}"
    fi
  done <<< "${filenames}"

  echo "Per-bread image files created."
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
    -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '${DATABASE_NAME}' AND table_name IN ('users', 'store', 'favourite_store', 'orders', 'order_item');"
)"

if [[ "${SCHEMA_READY}" != "5" ]]; then
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

create_per_bread_images

cat <<'EOF'
Test data applied.

Token note
- Access/refresh tokens are not inserted by this seed script.
- Run /api/user/login with a sample account; the app will issue tokens and save refresh_token.

Sample accounts
- demo-user@todaybread.com / todaybread123
- demo-boss1@todaybread.com ~ demo-boss10@todaybread.com / todaybread123

Recommended nearby query
- lat=37.4980950
- lng=127.0276100
- radius=5

Image replacement
- Place real images in scripts/seed-images/ with names like:
  seed_store_01.jpg, seed_bread_01.jpg, etc.
- Re-run this script to replace SVG placeholders.
EOF
