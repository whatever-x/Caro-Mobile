#!/usr/bin/env bash
# ============================================================
# Swagger 스펙 다운로드 (macOS / Linux)
# 인증: HTTP Basic (ID/PW)
# 자격증명: 환경변수 우선, 없으면 .swagger-credentials 파일
# ============================================================
set -euo pipefail

ARG_SWAGGER_URL="${1:-}"
# 스크립트 자기 위치 기준으로 자격증명 파일을 찾는다 (작업 디렉토리/에이전트 무관).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CRED_FILE="$SCRIPT_DIR/../.swagger-credentials"

# --- 1. .swagger-credentials 파일이 있으면 로드 (환경변수가 없는 키만 채움) ---
if [[ -f "$CRED_FILE" ]]; then
  while IFS='=' read -r key value; do
    [[ -z "$key" || "$key" == \#* ]] && continue
    key="$(echo "$key" | xargs)"          # 공백 제거
    if [[ -z "${!key:-}" ]]; then         # 환경변수가 이미 있으면 덮어쓰지 않음
      export "$key=$value"
    fi
  done < "$CRED_FILE"
fi

# --- 2. Swagger URL 결정 (인자 > 환경변수/파일에서 로드된 SWAGGER_URL) ---
SWAGGER_URL="${ARG_SWAGGER_URL:-${SWAGGER_URL:-}}"
if [[ -z "$SWAGGER_URL" ]]; then
  echo "ERROR: Swagger 스펙 URL이 없습니다." >&2
  echo "인자로 전달하거나, 환경변수 SWAGGER_URL 또는 .swagger-credentials의 SWAGGER_URL 키를 설정하세요." >&2
  echo "자세한 방법: reference/credentials-setup.md" >&2
  exit 1
fi

# --- 3. 필수 자격증명 확인 ---
if [[ -z "${SWAGGER_USER:-}" || -z "${SWAGGER_PASS:-}" ]]; then
  echo "ERROR: 자격증명이 없습니다." >&2
  echo "환경변수(SWAGGER_USER/SWAGGER_PASS)를 설정하거나" >&2
  echo ".swagger-credentials 파일을 만드세요." >&2
  echo "자세한 방법: reference/credentials-setup.md" >&2
  exit 1
fi

# --- 4. HTTP Basic 으로 스펙 가져오기 ---
curl -sSfL -u "${SWAGGER_USER}:${SWAGGER_PASS}" \
  "$SWAGGER_URL" -o openapi.json

# --- 5. 받은 게 진짜 OpenAPI 스펙인지 검증 ---
# (로그인 실패 시 서버가 HTML 로그인 페이지를 200으로 줄 수 있음)
if ! jq -e '.openapi // .swagger' openapi.json >/dev/null 2>&1; then
  echo "ERROR: 유효한 OpenAPI 스펙이 아닙니다." >&2
  echo "로그인 실패로 HTML 페이지가 반환됐을 가능성이 큽니다." >&2
  head -c 300 openapi.json >&2; echo >&2
  exit 1
fi

echo "다운로드 완료: spec version $(jq -r '.info.version' openapi.json)"
