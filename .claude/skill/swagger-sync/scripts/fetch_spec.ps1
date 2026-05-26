# ============================================================
# Swagger 스펙 다운로드 (Windows / PowerShell)
# 인증: HTTP Basic (ID/PW)
# 자격증명: 환경변수 우선, 없으면 .swagger-credentials 파일
# ============================================================
param(
    [string]$SwaggerUrl
)
$ErrorActionPreference = "Stop"
$CredFile = ".claude/skill/swagger-sync/.swagger-credentials"

# --- 1. .swagger-credentials 파일이 있으면 로드 (환경변수가 없는 키만 채움) ---
if (Test-Path $CredFile) {
    Get-Content $CredFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { return }
        $idx = $line.IndexOf("=")
        if ($idx -lt 1) { return }
        $key = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1)
        # 환경변수가 이미 있으면 덮어쓰지 않음
        if (-not [Environment]::GetEnvironmentVariable($key)) {
            Set-Item -Path "Env:$key" -Value $value
        }
    }
}

# --- 2. Swagger URL 결정 (인자 > 환경변수/파일에서 로드된 SWAGGER_URL) ---
if (-not $SwaggerUrl) { $SwaggerUrl = $env:SWAGGER_URL }
if (-not $SwaggerUrl) {
    Write-Error @"
Swagger 스펙 URL이 없습니다.
인자로 전달하거나, 환경변수 SWAGGER_URL 또는 .swagger-credentials의 SWAGGER_URL 키를 설정하세요.
자세한 방법: reference/credentials-setup.md
"@
    exit 1
}

# --- 3. 필수 자격증명 확인 ---
if (-not $env:SWAGGER_USER -or -not $env:SWAGGER_PASS) {
    Write-Error @"
자격증명이 없습니다.
환경변수(SWAGGER_USER/SWAGGER_PASS)를 설정하거나
.swagger-credentials 파일을 만드세요.
자세한 방법: reference/credentials-setup.md
"@
    exit 1
}

# --- 4. HTTP Basic 으로 스펙 가져오기 ---
# curl.exe 사용 이유:
#   - Invoke-WebRequest -OutFile은 한글 응답을 코드페이지로 잘못 저장해 mojibake 발생
#   - Invoke-WebRequest는 리다이렉트 시 Authorization 헤더를 자동 제거
#   - bash 스크립트와 동일한 도구를 사용해 동작 일관성 확보
# curl.exe는 Windows 10 1803+ 에 기본 탑재되어 있다.
# --fail: HTTP 4xx/5xx 시 종료코드 22로 실패
# --no-location: 리다이렉트를 따라가지 않음 (SWAGGER_URL이 UI 페이지면 명확히 실패)
& curl.exe --silent --show-error --fail --no-location `
    --user "$($env:SWAGGER_USER):$($env:SWAGGER_PASS)" `
    --output "openapi.json" `
    $SwaggerUrl
if ($LASTEXITCODE -ne 0) {
    Write-Error @"
스펙 다운로드 실패 (curl exit $LASTEXITCODE).
- 자격증명을 확인하세요 (SWAGGER_USER / SWAGGER_PASS)
- SWAGGER_URL이 스펙 JSON 엔드포인트(/v3/api-docs 등)를 가리키는지 확인하세요.
  Swagger UI 페이지(/swagger-ui/index.html)는 리다이렉트로 인해 실패합니다.
"@
    exit 1
}

# --- 5. 받은 게 진짜 OpenAPI 스펙인지 검증 ---
try {
    $spec = Get-Content "openapi.json" -Raw -Encoding utf8 | ConvertFrom-Json
    if (-not $spec.openapi -and -not $spec.swagger) { throw "스펙 키 없음" }
} catch {
    Write-Error @"
유효한 OpenAPI 스펙이 아닙니다.
로그인 실패로 HTML 페이지가 반환됐을 가능성이 큽니다.
"@
    exit 1
}

Write-Host "다운로드 완료: spec version $($spec.info.version)"
