# Screenshot Scenario 목록

이 디렉터리의 `scenarios.json`은 Android/iOS 스크린샷 테스트 시나리오의 명시적 목록입니다. 현재 9개 feature 모듈의 14개 시나리오가 등록되어 있습니다. 각 모듈의 테스트가 생성한 PNG 이름과 Roborazzi 비교 결과를 이 목록으로 검증합니다.

수집·비교·게시 로직은 별도 Python 스크립트 없이 GitHub Actions 워크플로에 있습니다.

| 워크플로 | 역할 |
| --- | --- |
| [screenshot-golden.yml](../../.github/workflows/screenshot-golden.yml) | develop의 전체 이미지를 기록·검증하고 90일 Golden Artifact로 보관 |
| [screenshot-pr.yml](../../.github/workflows/screenshot-pr.yml) | 정확한 PR base SHA의 Golden을 복원하고 Roborazzi compare task로 변경·신규 이미지를 생성 |
| [screenshot-comment.yml](../../.github/workflows/screenshot-comment.yml) | 변경 이미지만 전용 브랜치에 게시하고 기존 액션으로 PR 고정 댓글 생성·갱신 |

로컬 실행 방법, 결과 경로, 신규·삭제·무변경 처리와 워크플로 활성화 조건은 [Screenshot Test 가이드](../../.github/screenshot-test.md)를 참고하세요.

화면·상태를 추가할 때는 해당 feature의 결정적인 `commonTest` 시나리오와 Android/iOS 캡처 테스트를 작성한 뒤 이 목록에 ID를 등록합니다. ID는 영문·숫자로 시작하고 영문·숫자·점·밑줄·하이픈만 사용합니다. `_actual` 및 `_compare` 접미사는 Roborazzi가 사용하므로 시나리오 ID에 사용하지 않습니다.

이미지 원본과 보고서는 `build/` 아래 재생성 가능한 산출물입니다. feature 브랜치에 PNG를 커밋하지 않습니다.

## CI 실행 시간

- Golden/PR 모두 `scenarios.json`의 실제 렌더링 테스트에만 Gradle `--rerun`을 적용합니다. 전체 `--rerun-tasks`는 사용하지 않으므로 컴파일·링크 task의 재사용 판단은 Gradle에 맡깁니다.
- `setup-gradle`은 Gradle User Home 캐시를 읽고 씁니다. iOS의 `~/.konan`은 별도 캐시이며 OS·CPU 아키텍처·Xcode 버전/빌드·버전 카탈로그 해시로 구분합니다. 서로 다른 툴체인의 캐시로 fallback하지 않습니다.
- 실행별 `screenshot-profile-android/ios` Artifact(14일)에서 Gradle 단계별 시간을 확인합니다. 캐시 적중 여부는 `Cache Kotlin/Native`와 `setup-gradle` 로그에 기록됩니다.
- 성능 비교는 같은 앱 코드·러너·Xcode 조건에서 최초 실행과 캐시 적중 실행을 구분합니다. worker 수는 iOS에서만 비교하고 측정 근거 없이 제한하지 않습니다.
