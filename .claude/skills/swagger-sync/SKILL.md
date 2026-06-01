---
name: swagger-sync
description: >
  Swagger/OpenAPI 스펙에서 Kotlin 클라이언트 DTO와 태그별 Ktorfit API 인터페이스를
  생성하고, 사내 코딩 규칙을 적용한 뒤 변경사항이 있으면 브랜치 + 커밋까지 만든다.
  "DTO 동기화", "API 스펙 반영", "Swagger 업데이트", "DTO PR 만들어줘" 같은 요청 시
  반드시 이 스킬을 사용한다.
  Swagger 스펙이 ID/PW 로그인으로 보호되어 있어도 이 스킬로 처리한다.
---

# Swagger → Kotlin DTO + API 동기화

## 개요

이 스킬은 Swagger/OpenAPI 스펙에서 다음 두 가지를 자동으로 만들어낸다.

1. **Kotlin DTO** — 요청/응답 데이터 클래스 (`core/remote/dto/{tag}/{request|response}/`)
2. **Ktorfit API 인터페이스** — 태그별 호출 인터페이스 + Koin `apiModule` 바인딩
   (`core/remote/api/{Tag}Api.kt`, `core/remote/di/ApiModule.kt`)

cron 자동화는 사용하지 않으며, 사용자가 트리거할 때마다
Claude가 아래 절차를 수행한다.

## 사전 조건: Swagger 인증

이 Swagger 스펙은 **HTTP Basic (ID/PW)** 으로 보호되어 있다.
필요한 값은 `SWAGGER_USER`, `SWAGGER_PASS`, `SWAGGER_URL` 세 가지뿐이다.
자격증명은 두 가지 방법으로 공급할 수 있다.

1. **환경변수 방식 (권장 / 자동화 호환)** — 터미널에서 `export`로 주입.
2. **로컬 파일 방식 (편의용)** — `.swagger-credentials` 파일에 저장.
   이 파일은 `.gitignore`에 등록되어 git에 커밋되지 않는다.

`scripts/fetch_spec.sh`(mac) / `scripts/fetch_spec.ps1`(windows)는
**환경변수를 먼저 확인하고, 없으면 `.swagger-credentials` 파일을 읽는다.**
둘 다 없으면 에러로 중단한다.

> n8n 등 자동화 도구에서 실행할 때 `.swagger-credentials` 파일이 없어
> 문제가 생기면, 환경변수 방식으로 전환한다. 환경변수는 모든 환경에서 동작한다.

자세한 자격증명 설정 방법은 `reference/credentials-setup.md`를 참고한다.

## 절차

1. Swagger 스펙 URL을 결정한다. 우선순위는 다음과 같다.
   1. 사용자가 대화에서 직접 알려준 URL
   2. 환경변수 `SWAGGER_URL`
   3. `.swagger-credentials` 파일의 `SWAGGER_URL` 키
   위 세 가지 중 어느 것도 없을 때만 사용자에게 묻는다.
   (자세한 설정 방법은 `reference/credentials-setup.md` 참고)

2. 운영체제를 확인하고 알맞은 스크립트를 사용한다.
   - macOS / Linux → `scripts/*.sh`
   - Windows → `scripts/*.ps1`

3. `fetch_spec` 스크립트 실행 → `openapi.json` 확보.
   - 스크립트는 환경변수 또는 `.swagger-credentials` 파일에서 자격증명을 읽는다.
   - 스펙의 `info.version`을 사용자에게 보고한다.
   - 자격증명이 없어 스크립트가 중단되면, 추측하지 말고
     `reference/credentials-setup.md`를 안내한다.

4. `generate` 스크립트 실행 → `build/generated`에 raw DTO 생성.

5. 이동 전, 기존 DTO 디렉토리와 새 코드를 비교해
   breaking change(필드 삭제, 타입 변경, enum 값 제거)를 식별한다.

6. 생성된 모델을 `:core:remote` 모듈의
   `src/commonMain/kotlin/com/whatever/caro/core/remote/dto`로 옮긴다.
   이때 `reference/rules.md`의 DTO 섹션 규칙을 **반드시 적용**한다.

7. **태그별 Ktorfit API 인터페이스를 매핑한다.**
   - 스펙의 `tags[].name` 별로 `core/remote/api/{Tag}Api.kt`를 만들거나 갱신한다.
   - `reference/rules.md`의 "Ktorfit API 인터페이스 매핑" 섹션을 **반드시 적용**한다
     (어노테이션, 메서드 네이밍, 반환 타입 unwrap, 자동 헤더 제외 등).
   - 인증이 필요한 엔드포인트는 AUTH qualifier로,
     명시적으로 `security: []`인 엔드포인트만 NON_AUTH qualifier로
     `core/remote/di/ApiModule.kt`의 `apiModule`에 binding을 등록/갱신한다.
   - 스펙에서 사라진 엔드포인트/태그는 호출부 정리까지 함께 한다.

8. `verify` 스크립트 실행 → 컴파일 검증.
   - 실패 시: 원인을 분석해 수정 후 재시도. 3회 실패하면 중단하고 사용자에게 보고.

9. `git diff --quiet` 로 변경 여부 확인.
   - 변경 없음 → 사용자에게 "변경 없음" 보고하고 종료.
   - 변경 있음 → 10단계 진행.

10. 브랜치 생성 → 커밋까지 진행하고 사용자에게 알린다.

## 절대 규칙

- src/commonMain/kotlin/com/whatever/caro/core/model은 절대 수정/삭제하지 않는다.
- 컴파일 검증을 통과하지 못한 코드로는 브랜치 생성과 커밋을 하지 않는다.
- 브랜치는 feat/swagger-dto-update로 생성한다.
- 자격증명(ID/PW)을 SKILL.md, 스크립트, reference 등 어떤 파일에도 저장하지 않는다.
  자격증명은 환경변수 또는 `.gitignore`에 등록된 `.swagger-credentials` 파일로만 다룬다.
- 자격증명을 PR 본문, 커밋 메시지, 로그, 채팅 출력에 노출하지 않는다.
- 사용자가 채팅으로 ID/PW를 보내더라도 스크립트나 파일에 적어넣지 않는다.
  대신 `reference/credentials-setup.md`의 설정 방법을 안내한다.

## 참고 파일

- `reference/rules.md` — 사내 Kotlin DTO 코딩 규칙 + Ktorfit API 매핑 컨벤션
  (DTO 이동 단계와 API 매핑 단계에서 모두 적용)
- `reference/credentials-setup.md` — Swagger 자격증명 설정 방법
