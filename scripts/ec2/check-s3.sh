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

if ! command -v aws >/dev/null 2>&1; then
  echo "aws CLI is not installed." >&2
  exit 1
fi

aws sts get-caller-identity
aws s3 ls "s3://${S3_BUCKET}"

TEST_FILE="/tmp/todaybread-s3-test.txt"
TEST_KEY="todaybread-s3-test.txt"

echo "s3 test from ec2" > "${TEST_FILE}"
aws s3 cp "${TEST_FILE}" "s3://${S3_BUCKET}/${TEST_KEY}"
aws s3 rm "s3://${S3_BUCKET}/${TEST_KEY}"
rm -f "${TEST_FILE}"

echo "S3 check completed."
