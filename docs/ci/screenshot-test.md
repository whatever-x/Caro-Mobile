# 스크린샷 CI

Android/iOS 공통 Compose UI의 한국어·영어 캡처를 정확한 PR base SHA의 Golden과 비교합니다. 변경 원본은 PR에 게시합니다.
CI·Golden·수동 실행·게시 승인 절차의 원본 문서입니다. 작성 절차와 코드·명령 예제는
[스크린샷 작성 스킬](../../.agents/skills/caro-screenshot-testing/SKILL.md), 공통 규칙은
[스크린샷 테스트 규칙](../development/screenshot-testing.md)을 따릅니다.

## 캡처 계약

[scenarios.json](../../scripts/screenshot/scenarios.json)은 실행 코드와 대조할 기대 ID 목록입니다.
언어·기기 값은 각 feature의 `commonTest/ScreenshotCapture.kt`에 Kotlin으로 직접 선언합니다. 설정 JSON·코드 생성은 사용하지 않습니다.
세 워크플로의 Bash·jq는 기대 언어·프로필 ID와 파일명을 검사합니다. 이 이름을 바꾸면 Kotlin과 CI의 파일명 규칙을 함께 수정합니다.
공통 ID N개를 ko/en으로 확장하면 플랫폼별 2N개, 전체 4N개의 캡처가 됩니다.
현재 기대 목록은 9개 모듈·24개 논리 ID이며, 플랫폼별 48개·전체 96개입니다. 실제 출력 수와 구분합니다.
작은 화면·큰 글꼴 시나리오는 명시된 variant 하나를 사용합니다. 모든 시나리오를 모든 variant로 곱하지 않습니다.
파일명은 `<scenario-id>__<language>__<profile-id>.png`입니다. 플랫폼은 출력 경로와 메타데이터에 포함합니다.

[Golden](../../.github/workflows/screenshot-golden.yml)·[PR 비교](../../.github/workflows/screenshot-pr.yml)·
[게시](../../.github/workflows/screenshot-comment.yml) 워크플로가 Bash·jq로 필요한 검사를 수행합니다.
별도 스크립트·CLI·fixture 파일은 없습니다. 실행할 화면 테스트는 기존 화면별 공통 시나리오와 Android/iOS 래퍼에 있습니다.
언어·프로필 ID와 registry는 `metadata.json`에 포함합니다. 기기 설정 원본이나 실행 관측값을 담는 파일은 아닙니다. 별도 snapshot은 만들지 않습니다.

## 세 워크플로와 권한

1. **Screenshot Golden** (`screenshot-golden.yml`): 모든 `develop` push에서 두 플랫폼을 기록합니다.
   등록 ID를 언어·프로필로 확장합니다. 누락·중복·미등록 파일명을 검사합니다.
   `screenshot-golden-android/ios` 아티팩트는 PNG와 `metadata.json`을 90일 보관합니다.
2. **Screenshot PR** (`screenshot-pr.yml`): `develop`에서 수동 실행합니다. PR head/base SHA를 고정합니다. 병합 커밋의 부모가
   `[base, head]`인지 확인합니다. 체크아웃 대상은 **고정한 병합 커밋**입니다. PR head로 바꾸지 않습니다.
   읽기 권한으로 정확한 base SHA의 Golden을 복원합니다. 두 플랫폼의 Roborazzi 비교를 실행합니다.
   캐시를 유지하면서 registry에 있는 각 feature 테스트 태스크에만 `--rerun`을 붙입니다.
3. **Screenshot Comment** (`screenshot-comment.yml`): 비교 완료의 `workflow_run`으로 실행합니다.
   비교가 실패해도 요청 identity 아티팩트로 PR을 식별합니다. 이미지·댓글은 비교가 성공하고 보고서·현재 PR 검증이 통과해야 게시합니다.
   기본 브랜치의 **신뢰한 워크플로 코드**만 실행합니다. 게시용 소스 체크아웃은 하지 않습니다.
   고정한 병합 SHA의 registry는 API로 읽어 보고서와 대조합니다. PR이나 다운로드한 아티팩트의 실행 코드는 불러오지 않습니다.

게시 작업은 Actions 읽기와 contents/PR 쓰기 권한을 분리합니다. 같은 저장소의 열린 PR이 develop을 대상으로 하고,
현재 head/base가 승인된 값과 일치할 때만 이미지를 게시합니다. 이미지 push 전과 각 플랫폼 댓글 쓰기 전에 다시 검사합니다.
기대 파일명·경로·중복·regular file·symlink 금지·메타데이터와 허용 목록을 통과한 파일만 게시합니다.
`workflow_run.head_sha`는 dispatch 브랜치 SHA이므로 PR head 식별에 사용하지 않습니다.

이 워크플로는 저장소 기본 브랜치에 반영되어야 동작합니다. 병합 전 로컬 검사를 GitHub 수동 실행·렌더링·댓글 게시·Slack 수신의
확인 근거로 보고하지 않습니다. fork PR은 비교와 아티팩트 생성은 가능하지만 이미지·댓글은 게시하지 않습니다.

## 정확한 Golden과 호환성

Golden 조회는 `branch=develop&head_sha=<PR-base-SHA>`의 실행만 찾습니다. 진행 중이면 제한된 시간 동안 기다립니다.
완료 후 성공 여부를 확인합니다. 일치 실행 부재, 실패·취소, 10분 대기 초과, 플랫폼 아티팩트 누락·만료는 실패입니다.
같은 SHA의 실패한 실행을 다른 SHA의 성공 실행이나 PR 출력으로 대체하지 않습니다.

v5 Golden metadata에는 다음 값만 포함합니다.

- schema version 5, source SHA(정확한 PR base), platform
- languages `["ko", "en"]`, 기본·small-large-font·narrow profile ID
- scenarios: 모듈별 기대 논리 ID registry

다운로드한 파일 목록을 registry에서 확장한 기대 목록과 대조한 뒤 복원합니다. schema·SHA·플랫폼·언어·프로필 ID가 다르면 실패합니다.
같은 호환성 기준에서 새 ID가 생긴 경우에만 `added`로 허용합니다.
이미 Golden registry에 있는 캡처가 `added`로 보고되면 기존 기준 이미지 누락으로 실패합니다.
삭제 ID는 baseline과 현재 기대 목록의 차이로 계산합니다.

### v5 Golden 전환

기존 `<ID>.png` Golden과 v2/v3/v4 artifact는 v5 형식과 호환되지 않습니다. 별도 registry/config snapshot도 허용하지 않습니다.
새 파일명을 모두 신규 이미지로 취급하지 않고 metadata 누락 또는 schema·identity 오류로 중단합니다.
이 전환 변경은 로컬에서 검증할 수 있지만 기존 스키마의 exact-base Golden과 새 캡처를 비교할 수는 없습니다.

별도로 승인받아 develop에 병합한 뒤 **실제 병합 결과 SHA**의 자동 push Golden이 v5 기준을 생성해야 합니다.
해당 원본의 시각 검토와 Golden 성공을 확인한 후 다음 PR에서 exact-base 비교를 수행합니다.
이전 SHA를 재실행해도 새 스키마 코드는 생기지 않습니다. 다른 SHA의 Golden으로 대체하지 않습니다.
profiles 목록에는 Library의 `narrow`도 포함합니다. 이전 v5 Golden에 이 프로필이 없으면 기본 호환성 검사에서 실패합니다.
언어·프로필 ID·schema를 바꾸면 같은 호환성 경계를 다시 적용합니다. 같은 프로필 ID의 기기값 변경은 CI metadata로 판별하지 못합니다.

## 결과와 댓글

픽셀 차이는 report-only입니다. 컴파일·렌더링·동작 단언·등록·파일·Golden·보고서·게시 오류는 CI를 실패시킵니다.
CI는 기대 파일명·결과 목록·비어 있지 않은 regular file·symlink 금지·게시 허용 목록을 검사합니다.
PNG header·내용·치수·CRC·픽셀 디코딩, JUnit geometry 로그, 중복 JSON member key 검사는 하지 않습니다.
JSON 구문과 배열의 중복 ID/결과는 jq로 검사하지만 중복 object key는 마지막 값으로 해석할 수 있습니다.
metadata의 languages·profiles는 기대 라벨입니다. 실제 로케일·치수·density/fontScale·기기 모델·OS의 측정값으로 보고하지 않습니다.
기기값·root·실제 리소스 전환 단언은 기존 Kotlin 테스트에 유지합니다.

| 결과 | 플랫폼 댓글 |
| --- | --- |
| 기존 캡처 변경 | 정확한 base의 Before와 변경된 After 원본 |
| 캡처 추가 | After만 표시 |
| 캡처 삭제 | 시나리오·언어·프로필을 텍스트로 표시 |
| 플랫폼 변경 없음 | 기존 플랫폼 댓글만 `시각적 변경 없음`으로 갱신. 기존 댓글이 없으면 생성 생략 |

Android 댓글 제목은 **Android 스크린샷**, 마커는 `<!-- caro-screenshot-report:android -->`입니다.
iOS 댓글 제목은 **iOS 스크린샷**, 마커는 `<!-- caro-screenshot-report:ios -->`입니다.
각 댓글은 자기 플랫폼의 scenario/language/profile만 포함합니다. 이전 통합 마커 `<!-- caro-screenshot-report -->` 댓글은 조회·수정·삭제 대상으로
사용하지 않습니다. 봇 작성자와 정확한 플랫폼 마커를 검사합니다. 플랫폼 댓글이 중복되면 임의로 선택하지 않고 실패합니다.

변경·추가 이미지만 게시합니다. `_before.png`는 Golden 원본이고 `_actual.png`는 실제 변경 원본입니다. 원본을 합성하거나 자르지 않습니다.
두 플랫폼의 이미지를 하나의 `screenshots/pr-<number>` 독립 이력 브랜치에 **한 번** push한 뒤 두 댓글을 갱신합니다.
이미지 URL에는 이 게시 커밋의 immutable SHA를 사용합니다. 이 브랜치는 develop에 병합하지 않습니다. 현재 자동 삭제하지 않습니다.
이미지 변경이 없으면 push를 생략합니다. 삭제만 있는 보고서는 텍스트 댓글로 갱신합니다.

`screenshot-before-after-android/ios`에는 게시용 원본과 summary를, `screenshot-report-android/ios`에는 비교 진단 이미지와 같은 summary를 보관합니다.
Roborazzi HTML/JSON과 Gradle profile도 별도 아티팩트에 보관합니다. 실패해도 생성된 진단 자료는 업로드합니다.
HTML/JSON 보존은 90일, Gradle profile은 14일입니다. 새 Golden 기록이나 record 후 compare 통과는 시각적 승인을 대신하지 않습니다.

## Slack 집계

게시 작업 다음의 `Slack Notify Screenshot Result`는 기존 `SLACK_WEBHOOK_URL`을 사용합니다.
Android/iOS 작업 결과, 실패 단계, 게시 실패·생략·취소, PR·실행·아티팩트 링크를 기존 Block Kit로 집계합니다.
일부 작업 재실행 시 해당 실행 차수까지 각 플랫폼의 최신 결과를 사용합니다. 수동 메시지 게시나 새 secret 설정은 필요하지 않습니다.
fork PR의 상태·로그도 알리지만 이미지·PR 댓글은 게시하지 않습니다. 실제 수신은 CI 실행 결과와 분리해 확인합니다.

## 수동 실행과 에이전트 승인

PR 생성·업데이트만으로 스크린샷 비교를 시작하지 않습니다. Golden은 정확한 base SHA의 기준 이미지를 조회하도록 모든 `develop` push에서 생성합니다.
에이전트는 PR 생성 후 실제 diff와 시나리오 범위를 [PR 스크린샷 정책](../../.agents/hooks/pr-screenshot/pr-screenshot.md#사용자-확인-기준)에 따라 확인합니다.

UI 영향이 있거나 불확실하면 실행 여부를 사용자에게 묻습니다. 명확한 비 UI 변경이면 생략 이유를 보고합니다.
사용자가 스크린샷 실행을 이미 명시적으로 요청했다면 승인으로 인정합니다. 미응답은 승인으로 취급하지 않습니다.
head/base SHA가 바뀌면 다시 확인합니다.

승인 후 GitHub CLI로 실행합니다. SHA 자리에는 사용자가 승인한 값을 입력합니다.

```bash
request_id=$(uuidgen)
gh workflow run screenshot-pr.yml --repo whatever-x/Caro-Mobile --ref develop \
  -f pr_number=168 \
  -f expected_head_sha=<approved-head-sha> \
  -f expected_base_sha=<approved-base-sha> \
  -f request_id="$request_id"
gh run list --repo whatever-x/Caro-Mobile --workflow screenshot-pr.yml \
  --event workflow_dispatch --branch develop --limit 100 --json databaseId,displayTitle,url \
  | jq --arg title "Screenshot PR #168 · $request_id" '.[] | select(.displayTitle == $title)'
```

실행 목록에 아직 없으면 나중에 다시 조회합니다. 확인 없이 중복 실행하지 않습니다.
`gh run watch <RUN_ID> --repo whatever-x/Caro-Mobile --exit-status`로 완료 여부를 확인합니다.
`Screenshot Comment` 실행 제목은 `Screenshot result · source <RUN_ID> · attempt <run_attempt>`입니다.
원본 실행 ID와 실행 차수가 일치하는지 확인한 뒤 이미지·댓글·Slack 전달 결과를 보고합니다.
비교 실행 차수는 `gh api repos/whatever-x/Caro-Mobile/actions/runs/<RUN_ID> --jq .run_attempt`로 확인합니다.
게시 실행은 `gh run list --repo whatever-x/Caro-Mobile --workflow screenshot-comment.yml --json databaseId,displayTitle,url,conclusion`로 조회합니다.

사용자는 GitHub에서 `develop`을 선택합니다. **Actions → Screenshot PR → Run workflow**에 PR 번호만 입력해 실행할 수 있습니다.

예상 head/base SHA는 선택 사항입니다. 입력한다면 둘 다 제공합니다. 둘 다 생략하면 준비 단계가 현재 PR의 SHA를 고정합니다.

요청 ID도 선택 사항입니다. 생략하면 실행 ID를 사용합니다. 에이전트는 항상 승인받은 SHA 쌍과 고유 요청 ID를 제공합니다.

준비 단계는 잘못된 입력, 닫힌 PR, 다른 대상 브랜치, 예상 SHA 불일치, 병합 커밋 조회 불가, 병합 부모 불일치를 거부합니다. 비교 단계는 체크아웃한 SHA를 검증하고 고정된 병합 커밋을 사용합니다. Golden이 없거나 만료되면 다른 SHA로 대체하지 않고 실패합니다.

비교가 실패해도 준비 단계의 요청 메타데이터로 대상 PR을 식별합니다.
게시 작업은 원본 실행 ID와 두 플랫폼 보고서가 메타데이터에 일치하는지 검증합니다. 이미지·댓글을 쓰기 전에 현재 PR을 다시 확인합니다.
변경되었거나 닫힌 PR과 포크 PR은 게시를 생략합니다. Slack은 비교 실패·게시 실패·게시 생략을 구분합니다.

유효한 메타데이터 생성 전에 실패하면 실행 링크를 제공합니다.
동시 실행 그룹은 비교는 PR별, 게시는 원본 실행별로 구분합니다. 서로 다른 PR의 실행을 취소하지 않습니다.

## 에이전트 PR 생성 훅

훅 원본은 `.agents/hooks/pr-screenshot/pr-screenshot.json`입니다. Git에는 공유 원본만 관리합니다.
사용하려면 에이전트의 프로젝트 로컬 또는 사용자 영역에 훅을 등록합니다. 개발자별 루트 `.codex/`·`.claude/` 등록본은 Git에서 제외합니다.
에이전트마다 같은 판단·승인 절차를 적용합니다.
선행작업·판단·승인·가드레일과 설정·동작은 [PR 스크린샷 코멘트 훅](../../.agents/hooks/pr-screenshot/pr-screenshot.md)을 참고합니다.
승인 규칙은 [사용자 확인 기준](../../.agents/hooks/pr-screenshot/pr-screenshot.md#사용자-확인-기준)을 따릅니다.
