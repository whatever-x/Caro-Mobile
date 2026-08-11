#!/usr/bin/env bash
# ============================================================
# Kotlin DTO 생성 (macOS / Linux)
# openapi.json → build/generated 에 모델만 생성
# ============================================================
set -euo pipefail

if [[ ! -f openapi.json ]]; then
  echo "ERROR: openapi.json이 없습니다. 먼저 fetch_spec.sh를 실행하세요." >&2
  exit 1
fi

npx @openapitools/openapi-generator-cli generate \
  -i openapi.json \
  -g kotlin \
  -o build/generated \
  --global-property models \
  --additional-properties=modelPackage=com.whatever.caro.core.remote.dto,serializationLibrary=kotlinx_serialization,sortModelPropertiesByRequiredFlag=true

echo "DTO 생성 완료: build/generated"
