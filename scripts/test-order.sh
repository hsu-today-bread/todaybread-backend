#!/usr/bin/env bash

set -euo pipefail

# ============================================================
# 주문 + 결제 테스트 스크립트
#
# 사용법:
#   ./scripts/test-order.sh          # stub 모드 (가짜 결제)
#   ./scripts/test-order.sh --toss   # 토스 연동 모드 (confirm 안내)
#
# 사전 조건:
#   1. docker compose up -d
#   2. ./gradlew bootRun (stub: Active profiles에 stub 설정)
#   3. ./scripts/test-data.sh
# ============================================================

BASE_URL="${BASE_URL:-http://localhost:8080}"
MODE="${1:-stub}"
EMAIL="demo-user@todaybread.com"
PASSWORD="todaybread123"

# 색상
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

ok()   { echo -e "${GREEN}✅ $1${NC}"; }
fail() { echo -e "${RED}❌ $1${NC}"; }
info() { echo -e "${CYAN}ℹ️  $1${NC}"; }
warn() { echo -e "${YELLOW}⚠️  $1${NC}"; }

json_field() {
  python3 -c "import sys,json; print(json.load(sys.stdin).get('$1',''))" 2>/dev/null <<< "$2"
}

json_selling_bread_field() {
  python3 -c "import sys,json; data=json.load(sys.stdin); items=[b for b in data if b.get('isSelling')]; print(items[0].get('$1','') if items else '')" 2>/dev/null <<< "$2"
}

echo ""
echo "=========================================="
echo " todaybread 주문 + 결제 테스트"
echo " 서버: ${BASE_URL}"
echo " 모드: $([ "${MODE}" = "--toss" ] && echo "토스 연동" || echo "stub (가짜 결제)")"
echo "=========================================="
echo ""

# ── 1. 서버 상태 확인 ──
echo "1. 서버 상태 확인..."
HEALTH=$(curl -s "${BASE_URL}/api/system/health" 2>/dev/null || true)
if [[ "${HEALTH}" != "UP" ]]; then
  fail "서버가 응답하지 않습니다. ./gradlew bootRun 으로 서버를 먼저 실행하세요."
  exit 1
fi
ok "서버 정상 (${HEALTH})"
echo ""

# ── 2. 로그인 ──
echo "2. 로그인 (${EMAIL})..."
LOGIN_RESP=$(curl -s -X POST "${BASE_URL}/api/user/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\"}")

TOKEN=$(json_field "accessToken" "${LOGIN_RESP}")
if [[ -z "${TOKEN}" ]]; then
  fail "로그인 실패. ./scripts/test-data.sh 로 더미 데이터를 먼저 삽입하세요."
  echo "   응답: ${LOGIN_RESP}"
  exit 1
fi
ok "로그인 성공"
echo ""

# ── 3. Client Key 조회 ──
echo "3. 토스 Client Key 조회..."
CK_RESP=$(curl -s "${BASE_URL}/api/payments/client-key")
CLIENT_KEY=$(json_field "clientKey" "${CK_RESP}")
if [[ -z "${CLIENT_KEY}" || "${CLIENT_KEY}" == "null" ]]; then
  warn "Client Key가 비어있습니다. .env에 TOSS_CLIENT_KEY를 설정하세요."
else
  ok "Client Key: ${CLIENT_KEY:0:15}..."
fi
echo ""

# ── 4. 주문할 빵 조회 ──
BREAD_RESP=$(curl -s "${BASE_URL}/api/bread/nearby?lat=37.4980950&lng=127.0276100&radius=5&sort=distance" \
  -H "Authorization: Bearer ${TOKEN}")
BREAD_ID=$(json_selling_bread_field "id" "${BREAD_RESP}")
BREAD_NAME=$(json_selling_bread_field "name" "${BREAD_RESP}")

if [[ -z "${BREAD_ID}" ]]; then
  fail "주문 가능한 빵 조회 실패"
  echo "   응답: ${BREAD_RESP}"
  echo "   ./scripts/test-data.sh를 먼저 실행했는지 확인하세요."
  exit 1
fi
ok "주문할 빵 조회 성공 (${BREAD_NAME}, breadId=${BREAD_ID})"
echo ""

# ── 5. 주문 생성 (바로 구매) ──
IDEM_KEY="test-$(date +%s)-${RANDOM}"
echo "5. 바로 구매 주문 생성 (${BREAD_NAME} 2개)..."
ORDER_RESP=$(curl -s -X POST "${BASE_URL}/api/orders/direct" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Idempotency-Key: ${IDEM_KEY}" \
  -H "Content-Type: application/json" \
  -d "{\"breadId\":${BREAD_ID},\"quantity\":2}")

ORDER_ID=$(json_field "orderId" "${ORDER_RESP}")
AMOUNT=$(json_field "totalAmount" "${ORDER_RESP}")
STATUS=$(json_field "status" "${ORDER_RESP}")

if [[ -z "${ORDER_ID}" || "${ORDER_ID}" == "" ]]; then
  fail "주문 생성 실패"
  echo "   응답: ${ORDER_RESP}"
  exit 1
fi
ok "주문 생성 성공"
echo "   주문 ID: ${ORDER_ID} | 금액: ${AMOUNT}원 | 상태: ${STATUS}"
echo ""

# ── 6. 결제 ──
if [[ "${MODE}" == "--toss" ]]; then
  echo "6. 결제 승인 확정 (토스 연동 모드)"
  echo ""
  warn "paymentKey가 필요합니다."
  info "프론트엔드에서 토스 SDK로 결제 위젯을 띄우면 paymentKey가 발급됩니다."
  info "토스 SDK 결제 요청 시 orderId는 \"order_${ORDER_ID}\" 로 설정하세요."
  echo ""
  echo "   paymentKey를 받은 후 아래 명령어를 실행하세요:"
  echo ""
  echo -e "   ${CYAN}curl -X POST ${BASE_URL}/api/payments/confirm \\\\${NC}"
  echo -e "   ${CYAN}  -H \"Authorization: Bearer ${TOKEN}\" \\\\${NC}"
  echo -e "   ${CYAN}  -H \"Idempotency-Key: pay-${IDEM_KEY}\" \\\\${NC}"
  echo -e "   ${CYAN}  -H \"Content-Type: application/json\" \\\\${NC}"
  echo -e "   ${CYAN}  -d '{\"paymentKey\":\"여기에_paymentKey\",\"orderId\":${ORDER_ID},\"amount\":${AMOUNT}}'${NC}"
  echo ""

else
  echo "6. 결제 요청 (stub 모드)..."
  PAY_RESP=$(curl -s -X POST "${BASE_URL}/api/payments" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Idempotency-Key: pay-${IDEM_KEY}" \
    -H "Content-Type: application/json" \
    -d "{\"orderId\":${ORDER_ID},\"amount\":${AMOUNT}}")

  PAY_STATUS=$(json_field "status" "${PAY_RESP}")
  if [[ "${PAY_STATUS}" == "APPROVED" ]]; then
    ok "결제 성공 (${PAY_STATUS})"
  else
    fail "결제 실패"
    echo "   응답: ${PAY_RESP}"
    exit 1
  fi
  echo ""

  # ── 7. 주문 상태 확인 ──
  echo "7. 주문 상태 확인..."
  DETAIL_RESP=$(curl -s "${BASE_URL}/api/orders/${ORDER_ID}" \
    -H "Authorization: Bearer ${TOKEN}")
  FINAL_STATUS=$(json_field "status" "${DETAIL_RESP}")
  ok "주문 상태: ${FINAL_STATUS}"
  echo ""

  # ── 8. 주문 취소 ──
  if [[ "${FINAL_STATUS}" == "CONFIRMED" ]]; then
    echo "8. 주문 취소..."
    CANCEL_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
      "${BASE_URL}/api/orders/${ORDER_ID}/cancel" \
      -H "Authorization: Bearer ${TOKEN}")

    if [[ "${CANCEL_CODE}" == "200" ]]; then
      ok "주문 취소 성공"
    else
      fail "주문 취소 실패 (HTTP ${CANCEL_CODE})"
    fi

    AFTER_RESP=$(curl -s "${BASE_URL}/api/orders/${ORDER_ID}" \
      -H "Authorization: Bearer ${TOKEN}")
    AFTER_STATUS=$(json_field "status" "${AFTER_RESP}")
    info "취소 후 상태: ${AFTER_STATUS}"
    echo ""
  fi
fi

echo "=========================================="
echo " 테스트 완료"
echo "=========================================="
