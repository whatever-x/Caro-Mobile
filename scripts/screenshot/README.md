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
