# PR CI 검사 안내

일반 PR 검사를 확인하거나 실패 원인을 확인할 때 읽습니다. 실행 구성의 원본은 [caro-ci.yml](../../.github/workflows/caro-ci.yml)입니다.

## 실행 조건과 구성

`main`·`develop` 대상 PR에서 실행합니다. 같은 워크플로·PR ref의 새 실행은 이전 실행을 취소합니다.

| job | 검사·처리 내용 |
| --- | --- |
| `lint-check` | `spotlessCheck --stacktrace`로 포맷 검사 |
| `test-coverage` | `allTests`, `koverAllXmlReport`, `koverAllVerify` 실행 후 PR 커버리지 댓글 게시 |
| `stability-check` | `:androidApp:assembleDebug` 빌드 후 `stabilityCheck` 실행 |
| `slack-notify` | 앞선 세 job의 결과를 모아 Slack 알림 전송 |

Gradle의 커버리지 검증 하한과 PR 댓글 표시 기준은 서로 다릅니다. [테스트 안내](../development/testing.md#커버리지)를 확인합니다. Slack 알림은 lint job 내부가 아닌 별도 job에서 수행합니다.

## 결과 확인

- 실패한 job과 최초 실패 step의 로그를 확인합니다.
- 테스트·커버리지 검증 결과와 PR 댓글 게시 결과를 구분합니다.
- 검사 결과와 Slack 알림 결과를 구분합니다.
- 로컬에서 재현한 결과를 실제 CI 성공으로 보고하지 않습니다.

로컬 빌드·포맷 명령은 [빌드 안내](../development/build.md), 테스트 명령은 [테스트 안내](../development/testing.md)를 따릅니다.

## 스크린샷 CI와의 구분

PR 스크린샷 비교는 일반 PR 검사와 별개이며 수동 실행합니다. Golden 생성·비교·이미지·댓글·Slack 절차는 [스크린샷 CI](screenshot-test.md)를 따릅니다. 실행 전 판단·승인 규칙은 [PR 정책 목록](../collaboration/git-workflow.md#pr-생성-시-확인할-정책)에서 확인합니다.
