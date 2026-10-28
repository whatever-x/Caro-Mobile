# 스크린샷 CI

Android/iOS 공통 Compose UI의 한국어·영어 캡처를 정확한 PR base SHA의 Golden과 비교하고, 변경 원본을 PR에 게시합니다.
CI·Golden·수동 실행·게시 승인 절차의 원본 문서입니다. 시나리오 작성·소스셋·로컬 명령·로케일·치수는
[스크린샷 테스트 개발](../development/screenshot-testing.md)을 따릅니다.

## 캡처 계약

`scenarios.json`은 실행 코드와 대조할 기대 ID 목록이고 `capture-config.json`은 언어·프로필의 단일 원본입니다.
공통 ID N개는 ko/en으로 확장되어 플랫폼별 2N개, 전체 4N개의 캡처가 됩니다.
작은 화면·큰 글꼴 시나리오는 명시된 variant 하나를 사용하며 모든 시나리오를 모든 variant로 곱하지 않습니다.
파일명은 `<scenario-id>__<language>__<profile-id>.png`입니다. 플랫폼은 출력 경로와 메타데이터에 포함합니다.

[`pipeline.py`](../../scripts/screenshot/pipeline.py)는 registry 확장, 고유 파일명·PNG 내용/치수·비교 결과·Golden 복원·게시 파일을 검증합니다.
실행 가능한 테스트는 feature의 공통 Kotlin 목록과 플랫폼 래퍼에 있습니다. JSON으로 테스트를 생성하지 않습니다.
Python 회귀 fixture는 이 실제 운영 함수를 호출하고 CLI를 실행합니다. 이는 로컬 계약 검증이며 CI 렌더링·게시 검증과 구분합니다.

## 세 워크플로와 권한

1. **Screenshot Golden** (`screenshot-golden.yml`): 모든 `develop` push에서 두 플랫폼을 기록합니다.
   등록 ID를 언어·프로필로 확장해 누락·중복·미등록 PNG와 치수를 검사합니다.
   `screenshot-golden-android/ios` 아티팩트는 PNG, `scenarios.json`, `capture-config.json`, `metadata.json`을 90일 보관합니다.
2. **Screenshot PR** (`screenshot-pr.yml`): `develop`에서 수동 실행합니다. PR head/base SHA를 고정하고 병합 커밋의 부모가
   `[base, head]`인지 확인합니다. 체크아웃 대상은 **고정한 병합 커밋**입니다. PR head로 바꾸지 않습니다.
   읽기 권한으로 exact-base Golden을 찾아 복원하고 두 플랫폼의 Roborazzi 비교를 실행합니다.
   캐시를 유지하면서 registry에 있는 각 feature 테스트 태스크에만 `--rerun`을 붙입니다.
3. **Screenshot Comment** (`screenshot-comment.yml`): 비교 완료의 `workflow_run`으로 실행합니다.
   요청 identity 아티팩트로 실패 시에도 PR을 식별합니다. 이미지·댓글은 비교가 성공하고 보고서·현재 PR 검증이 통과해야 게시합니다.
   기본 브랜치에서 실행 중인 **신뢰한 workflow SHA**(`github.workflow_sha`)의 scripts/config만 체크아웃합니다.
   PR이나 다운로드한 아티팩트의 스크립트를 실행하지 않습니다.

게시 작업은 Actions 읽기와 contents/PR 쓰기 권한을 분리해 사용합니다. 같은 저장소의 열린 PR이며 대상 브랜치가 develop이고,
현재 head/base가 승인된 값과 일치할 때만 이미지를 게시합니다. 이미지 push 전과 각 플랫폼 댓글 쓰기 전에 다시 검사합니다.
파일명·경로·중복·symlink·PNG와 메타데이터를 검사한 파일만 게시 대상으로 허용합니다.
`workflow_run.head_sha`는 dispatch 브랜치 SHA이므로 PR head 식별에 사용하지 않습니다.

이 워크플로는 저장소 기본 브랜치에 반영되어야 동작합니다. 병합 전 로컬 검사만으로 GitHub 수동 실행·렌더링·댓글 게시·Slack 수신을
확인했다고 보고하지 않습니다. fork PR은 비교와 아티팩트 생성은 가능하지만 이미지·댓글은 게시하지 않습니다.

## 정확한 Golden과 호환성

Golden 조회는 `branch=develop&head_sha=<PR-base-SHA>`의 실행만 찾습니다. 진행 중이면 제한된 시간 동안 기다리고,
완료된 실행이 성공인지 확인합니다. 일치 실행 부재, 실패·취소, 10분 대기 초과, 플랫폼 아티팩트 누락·만료는 실패입니다.
같은 SHA의 실패한 실행을 다른 SHA의 성공 실행이나 PR 출력으로 대체하지 않습니다.

v2 Golden identity에는 다음 값이 포함됩니다.

- schema version, 정규화한 `capture-config.json`의 SHA-256 hash
- Golden source SHA(정확한 PR base), platform, languages, profile ID·px 크기·density·fontScale
- registry에서 확장한 모든 캡처의 scenario·language·profile·filename과 검증한 `pngSize`

다운로드한 PNG와 registry·메타데이터를 대조한 뒤 복원합니다. schema/configuration이 다르거나 SHA·플랫폼·언어·프로필이 잘못되면
명확히 실패합니다. 같은 schema/configuration 안에서 새 ID가 생긴 경우에만 `added`로 허용합니다.
이미 Golden registry에 있는 캡처가 `added`로 보고되면 기존 기준 이미지 누락으로 실패합니다.
삭제 ID는 baseline과 현재 기대 목록의 차이로 계산합니다.

### v2 Golden 전환

기존 `<ID>.png` Golden은 언어·프로필·v2 identity가 없어 호환되지 않습니다.
새 파일명을 모두 신규 이미지로 취급하지 않고 `Legacy/missing Golden metadata` 또는 호환성 오류로 중단합니다.
이 전환 변경은 로컬에서 검증할 수 있지만 기존 스키마의 exact-base Golden과 새 캡처를 비교할 수는 없습니다.

별도로 승인받아 develop에 병합한 뒤 **실제 병합 결과 SHA**의 자동 push Golden이 v2 기준을 생성해야 합니다.
해당 원본의 시각 검토와 Golden 성공을 확인한 후 다음 PR에서 exact-base 비교를 수행합니다.
이전 SHA를 재실행해도 새 스키마 코드가 생기지 않으며, 다른 SHA의 Golden을 가져오는 우회는 허용하지 않습니다.
언어·프로필·치수 등 configuration을 바꾸면 같은 호환성 경계를 다시 적용합니다.

## 결과와 댓글

픽셀 차이는 report-only입니다. 컴파일·렌더링·동작 단언·등록·PNG·Golden·보고서·게시 오류는 CI를 실패시킵니다.
테스트가 기록한 root 크기와 density/fontScale은 로그의 `SCREENSHOT_GEOMETRY`로, 검증된 PNG 치수는 Golden/보고서 메타데이터로 확인합니다.
캡처 geometry와 테스트 호스트의 실제 기기 모델·OS 검증은 구분합니다.

| 결과 | 플랫폼 댓글 |
| --- | --- |
| 기존 캡처 변경 | 정확한 base의 Before와 변경된 After 원본 |
| 캡처 추가 | After만 표시 |
| 캡처 삭제 | 시나리오·언어·프로필을 텍스트로 표시 |
| 플랫폼 변경 없음 | 그 플랫폼의 기존 댓글만 `시각적 변경 없음`으로 갱신; 기존 댓글이 없으면 생성 생략 |

Android 댓글 제목은 **Android 스크린샷**, 마커는 `<!-- caro-screenshot-report:android -->`입니다.
iOS 댓글 제목은 **iOS 스크린샷**, 마커는 `<!-- caro-screenshot-report:ios -->`입니다.
각 댓글은 자기 플랫폼의 scenario/language/profile만 포함합니다. 이전 통합 마커 `<!-- caro-screenshot-report -->` 댓글은 조회·수정·삭제 대상으로
사용하지 않습니다. 봇 작성자와 정확한 플랫폼 마커를 검사하고, 중복 플랫폼 댓글이 있으면 임의로 선택하지 않고 실패합니다.

변경·추가 이미지만 게시하며 `_before.png`는 Golden 원본, `_actual.png`는 실제 변경 원본입니다. 원본을 합성하거나 자르지 않습니다.
두 플랫폼의 이미지를 하나의 `screenshots/pr-<number>` 독립 이력 브랜치에 **한 번** push한 뒤 두 댓글을 갱신합니다.
이미지 URL에는 이 게시 커밋의 immutable SHA를 사용합니다. 이 브랜치는 develop에 병합하지 않으며 현재 자동 삭제하지 않습니다.
이미지 변경이 없으면 push를 생략합니다. 삭제만 있는 보고서는 텍스트 댓글로 갱신합니다.

`screenshot-before-after-android/ios`에는 게시용 원본과 summary를, `screenshot-report-android/ios`에는 비교 진단 이미지와 같은 summary를 보관합니다.
Roborazzi HTML/JSON과 Gradle profile도 별도 아티팩트에 보관하며 실패 시에도 생성된 진단 자료는 업로드합니다.
HTML/JSON 보존은 90일, Gradle profile은 14일입니다. 새 Golden 기록이나 record 후 compare 통과는 시각적 승인을 대신하지 않습니다.

## Slack 집계

게시 작업 다음의 `Slack Notify Screenshot Result`는 기존 `SLACK_WEBHOOK_URL`을 사용합니다.
Android/iOS 작업 결과, 실패 단계, 게시 실패·생략·취소, PR·실행·아티팩트 링크를 기존 Block Kit로 집계합니다.
일부 작업 재실행 시 해당 실행 차수까지 각 플랫폼의 최신 결과를 사용합니다. 수동 메시지 게시나 새 secret 설정은 필요하지 않습니다.
fork PR의 상태·로그도 알리지만 이미지·PR 댓글은 게시하지 않습니다. 실제 수신은 CI 실행 결과와 분리해 확인합니다.

## 수동 실행과 에이전트 승인

PR 생성·업데이트만으로 스크린샷 비교를 시작하지 않습니다. 정확한 base SHA의 기준 이미지를 조회할 수 있도록 Golden 생성은 모든 `develop` push에서 계속 실행합니다. [PR 스크린샷 정책](../../.agents/hooks/pr-screenshot/pr-screenshot.md#사용자-확인-기준)에 따라 PR 생성 후 실제 diff와 시나리오의 검사 범위를 확인합니다. UI 영향이 있거나 불확실하면 사용자에게 실행 여부를 묻고, 명확한 비 UI 변경이면 생략 이유를 보고합니다. 사용자가 이미 스크린샷 실행을 명시적으로 요청했다면 승인으로 인정하며, 미응답은 승인으로 취급하지 않습니다. head/base SHA가 바뀌면 다시 확인합니다.

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

아직 실행 목록에 나타나지 않으면 나중에 다시 조회합니다. 확인 없이 중복 실행하지 않습니다. `gh run watch <RUN_ID> --repo whatever-x/Caro-Mobile --exit-status`로 완료 여부를 확인합니다. 별도의 `Screenshot Comment` 실행 제목은 `Screenshot result · source <RUN_ID> · attempt <run_attempt>`입니다. 원본 실행 ID와 실행 차수가 일치하는지 확인한 뒤 이미지·댓글·Slack 전달 결과를 보고합니다. 비교 실행 차수는 `gh api repos/whatever-x/Caro-Mobile/actions/runs/<RUN_ID> --jq .run_attempt`로 확인하고, 게시 실행은 `gh run list --repo whatever-x/Caro-Mobile --workflow screenshot-comment.yml --json databaseId,displayTitle,url,conclusion`로 조회합니다.

사용자는 GitHub에서 `develop`을 선택하고 **Actions → Screenshot PR → Run workflow**에 PR 번호만 입력해 실행할 수 있습니다. 예상 head/base SHA는 선택 사항이지만 입력할 때는 둘 다 제공해야 합니다. 둘 다 생략하면 준비 단계에서 현재 PR의 SHA를 고정합니다. 요청 ID도 선택 사항이며 생략하면 실행 ID를 사용합니다. 에이전트는 항상 승인받은 SHA 쌍과 고유 요청 ID를 제공합니다.

준비 단계는 잘못된 입력, 닫힌 PR, 다른 대상 브랜치, 예상 SHA 불일치, 병합 커밋 조회 불가, 병합 부모 불일치를 거부합니다. 비교 단계는 체크아웃한 SHA를 검증하고 고정된 병합 커밋을 사용합니다. Golden이 없거나 만료되면 다른 SHA로 대체하지 않고 실패합니다.

비교가 실패해도 준비 단계의 요청 메타데이터로 대상 PR을 식별합니다. 게시 작업은 원본 실행 ID와 두 플랫폼의 보고서가 메타데이터에 일치하는지 검증한 뒤, 이미지·댓글을 쓰기 전에 현재 PR을 다시 확인합니다. 변경되었거나 닫힌 PR과 포크 PR은 게시를 생략합니다. Slack에서는 비교 실패·게시 실패·게시 생략을 구분합니다. 유효한 메타데이터 생성 전에 실패하면 실행 링크를 제공합니다. 동시 실행 그룹은 비교는 PR별, 게시는 원본 실행별로 구분하므로 서로 다른 PR의 실행을 취소하지 않습니다.

## 에이전트 PR 생성 훅

훅 원본은 `.agents/hooks/pr-screenshot/pr-screenshot.json`입니다. 공유 원본만 Git으로 관리하며, 사용하려면 에이전트의 프로젝트 로컬 또는 사용자 영역에 훅을 등록해야 합니다. 루트 `.codex/`·`.claude/`의 개발자별 로컬 등록본은 Git에서 제외합니다. 사용 중인 에이전트에 동일한 판단·승인 절차를 적용합니다. 선행작업·판단·승인·가드레일과 필요한 설정·동작 레퍼런스는 [PR 스크린샷 코멘트 훅](../../.agents/hooks/pr-screenshot/pr-screenshot.md)을 참고합니다. 에이전트 승인 규칙은 [사용자 확인 기준](../../.agents/hooks/pr-screenshot/pr-screenshot.md#사용자-확인-기준)를 따릅니다.
