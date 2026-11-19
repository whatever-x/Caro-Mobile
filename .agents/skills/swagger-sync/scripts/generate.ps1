# ============================================================
# Kotlin DTO 생성 (Windows / PowerShell)
# openapi.json → build/generated 에 모델만 생성
# ============================================================
$ErrorActionPreference = "Stop"

if (-not (Test-Path "openapi.json")) {
    Write-Error "openapi.json이 없습니다. 먼저 fetch_spec.ps1을 실행하세요."
    exit 1
}

npx '@openapitools/openapi-generator-cli' generate `
    -i openapi.json `
    -g kotlin `
    -o build/generated `
    --global-property models `
    --additional-properties=modelPackage=com.whatever.caro.core.remote.dto,serializationLibrary=kotlinx_serialization,sortModelPropertiesByRequiredFlag=true

Write-Host "DTO 생성 완료: build/generated"
