# 스크린샷 테스트

`scripts/screenshot/scenarios.json`은 피처 모듈의 스크린샷 시나리오를 등록합니다(현재 29개). 고정된 `commonTest` 시나리오를 Android의 `androidHostTest`와 iOS의 `iosSimulatorArm64Test`에서 렌더링하며, ViewModel이나 API를 사용하지 않습니다.

## 로컬 실행

빌드에 필요한 `local.properties` 값을 준비한 뒤 실행합니다.

```bash
./gradlew recordRoborazziAndroidHostTest
./gradlew recordRoborazziIosSimulatorArm64
./gradlew compareRoborazziAndroidHostTest
./gradlew compareRoborazziIosSimulatorArm64
```

루트 태스크는 해당 태스크가 있는 모든 피처 모듈에서 실행됩니다. 기록 태스크는 로컬 기준 이미지를 생성하고, 비교 태스크는 그 이미지와 현재 화면을 비교합니다. 로컬 Xcode 27에서는 `spmForKmp` 호환성 때문에 SwiftPM의 네이티브 빌드 시스템이 필요할 수 있습니다. CI는 Xcode 26.6을 사용합니다.

`feature/*/build/outputs/roborazzi/<target>/`의 PNG는 Git에서 제외되는 빌드 산출물입니다. 피처 브랜치에 커밋하지 않습니다. Roborazzi는 각 모듈의 `build/test-results/roborazzi/<target>/results-summary.json`에 JSON 결과를, `build/reports/roborazzi/<target>/index.html`에 HTML 보고서를 생성합니다.

## GitHub Actions 실행 흐름

워크플로는 Roborazzi Gradle 태스크, Bash/`jq`, 아티팩트 액션과 [공식 PR 댓글 예제](https://github.com/takahirom/roborazzi-compare-on-github-comment-sample)의 댓글 액션을 사용합니다. PR 검증·렌더링·게시 로직은 워크플로 안에서 실행하며, 별도 Python이나 셸 스크립트는 필요하지 않습니다.

1. **Screenshot Golden** (`screenshot-golden.yml`): `develop`에 push할 때 Android/iOS 스크린샷을 기록합니다. PNG 이름을 `scenarios.json`과 대조하고, 전체 기준 이미지(Golden)를 아티팩트로 업로드해 90일간 보관합니다. 각 아티팩트에는 시나리오 목록도 포함됩니다.
2. **Screenshot PR** (`screenshot-pr.yml`): `develop` 대상의 열린 PR을 `workflow_dispatch`로 수동 검사합니다. PR의 head/base SHA와 병합 커밋을 고정하고 병합 커밋의 두 부모를 검증한 뒤 요청 메타데이터를 보관합니다. PR의 base SHA와 실행의 head SHA가 정확히 일치하는 성공한 Golden 실행을 찾아 다운로드합니다. 기준 이미지를 각 모듈의 Roborazzi 출력 디렉토리에 복원하고 `compareRoborazziAndroidHostTest` / `compareRoborazziIosSimulatorArm64`를 실행합니다. 등록된 모든 시나리오의 JSON 결과를 검증하고 변경·추가된 이미지만 수집합니다. Roborazzi HTML/JSON 진단 자료는 별도로 업로드하며, 실패 시에도 생성된 자료가 있으면 보관합니다.
3. **Screenshot Comment** (`screenshot-comment.yml`): 수동 비교가 완료되면 `workflow_run`으로 실행됩니다. 먼저 `screenshot-context` 아티팩트를 읽어 비교 실패 시에도 대상 PR을 식별합니다. 이미지 게시는 비교 성공 시에만 수행합니다. 보고서 파일을 검증하고 PR이 동일한 head/base SHA로 열려 있는지 확인한 뒤, `screenshots/pr-<number>`에 이미지를 게시합니다. `peter-evans/find-comment` / `peter-evans/create-or-update-comment`로 식별 마커가 있는 댓글 하나를 유지합니다. 액션 버전은 커밋 SHA로 고정합니다.
4. **Slack Notify Screenshot Result** (`screenshot-comment.yml` 내부): 비교가 완료되고 게시 단계가 끝나거나 생략되면 실행됩니다. 비교·게시 실패도 알림 대상입니다. 기존 `SLACK_WEBHOOK_URL`을 사용해 전체 스크린샷 CI 결과, Android/iOS 작업 결과, 실패한 단계 이름, PR·작업·아티팩트 링크를 전송합니다. Slack Block Kit의 제목과 구분선으로 결과·실패 원인·로그 링크를 나누고, 기본 버튼으로 PR을 엽니다. PR을 식별할 수 없으면 비교 실행을 엽니다. 메시지 집계와 v4.0.0 Slack 액션의 전송 데이터는 워크플로에 작성되어 있어 별도 셸·jq 파일은 필요하지 않습니다. 일부 작업만 재실행하면 알림 대상 실행 차수까지 각 플랫폼의 최신 결과를 사용합니다. 취소·생략은 실패와 구분하며, 픽셀 차이는 보고만 합니다. 이 알림은 기존 Caro Mobile CI 알림과 별도로 스크린샷 CI 결과를 전달합니다.

메시지 집계 단계는 출력값을 내보내기 전에 부분 재실행, 게시 실패, 취소, PR이 없을 때의 버튼 링크를 자체 검사합니다.

## 보고서 동작

- 기존 시나리오 변경: 정확한 PR base SHA의 Golden을 `_before.png`로 복사하고 Roborazzi의 `_actual.png`와 함께 Before/After 열에 표시합니다. 원본을 자르거나 합성하지 않습니다.
- 새 시나리오: 변경 후 이미지인 `_actual.png`만 표시합니다.
- 삭제된 시나리오: Golden과 PR의 시나리오 목록을 비교해 삭제 내역을 텍스트로 표시합니다.
- 변경 없음: 이미지 브랜치 게시와 새 댓글 생성을 생략합니다. 기존 스크린샷 댓글이 있으면 “시각적 변경 없음”으로 갱신합니다.

`screenshot-before-after-android/ios` 아티팩트에 게시용 원본과 요약을 보관합니다. 기존 `screenshot-report-android/ios` 아티팩트와 Roborazzi 진단 자료도 유지하며, `_compare.png`는 이 자료에만 포함됩니다. 게시 작업은 Before/After 아티팩트만 사용합니다.

픽셀 차이 자체는 보고만 하며 CI를 실패시키지 않습니다. 빌드·렌더링 실패, Golden 누락·만료, 등록된 결과·이미지 누락, 잘못된 보고서, 게시 오류는 CI 실패로 처리합니다. Golden이 없을 때 다른 커밋의 이미지로 대체하지 않습니다.

이미지 게시용 브랜치는 보고서 이미지만 포함하며 독립된 Git 이력을 사용합니다. 반복 실행은 같은 PR 전용 브랜치를 덮어씁니다. 이미지 링크에는 게시 커밋 SHA를 사용해 캐시된 이전 결과가 표시되지 않도록 합니다. 이 브랜치는 `develop`에 병합하지 않으며 현재 자동 삭제 없이 유지합니다.

## 적용 조건과 권한

`workflow_run` 워크플로는 저장소 기본 브랜치에 있어야 합니다. 수동 실행 및 댓글 워크플로를 기본 브랜치인 `develop`에 반영해야 합니다. 병합 전 로컬 검사만으로 실제 수동 실행·렌더링·게시·Slack 수신을 검증할 수는 없습니다.

PR 비교는 읽기 권한으로 고정된 PR 병합 커밋을 체크아웃합니다. `workflow_run.head_sha`는 수동 실행 브랜치의 SHA이므로 PR head 식별에 사용하지 않습니다. 별도 게시 작업은 쓰기 권한으로 기본 브랜치의 워크플로를 실행하며, PR이나 다운로드한 아티팩트의 코드·스크립트를 실행하지 않습니다. 게시는 같은 저장소의 PR로 제한합니다. 포크 PR은 비교와 아티팩트 생성은 가능하지만 이미지·댓글을 게시하지 않습니다.

Slack 알림은 `develop`에서 수동 실행한 작업을 대상으로 기본 브랜치에서 실행합니다. 포크 PR도 상태·로그 링크는 알리지만 이미지와 PR 댓글은 게시하지 않습니다. 실제 알림을 확인하려면 수정한 워크플로를 기본 브랜치에 먼저 반영해야 합니다. 추가 Slack 시크릿은 필요하지 않습니다.

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
