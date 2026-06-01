# ============================================================
# Kotlin 컴파일 검증 (Windows / PowerShell)
# 깨진 DTO로 PR이 생성되는 것을 방지
# ============================================================
$ErrorActionPreference = "Stop"

# DTO는 :core:remote에, 호출부는 :core:remote / :core:data 등에 있다.
# Android dev/debug 변형 컴파일은 의존 모듈의 commonMain/androidMain 컴파일까지 트리거하므로
# 이 한 태스크로 전체 DTO 변경 영향을 검증할 수 있다.
.\gradlew.bat :androidApp:compileDevDebugKotlin

Write-Host "컴파일 검증 통과"
