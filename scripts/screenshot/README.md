# 스크린샷 기대 목록

[scenarios.json](scenarios.json)은 feature 모듈별 기대 논리 ID입니다. 실행 코드는 각 feature의 화면별 `***ScreenshotScenario`와 Android/iOS 래퍼에 있습니다.
JSON을 수정해도 테스트가 생성되거나 실행되지는 않습니다.

```json
{"home": ["home.home.with_deck"], "login": ["login.login.initial"]}
```

등록된 모든 모듈을 통틀어 ID를 고유하게 작성합니다. 영문·숫자로 시작하고 영문·숫자·점·밑줄·하이픈을 사용합니다.
`__`와 `_actual`·`_compare`·`_before` 접미사는 예약합니다. 파일명은 `<scenario-id>__<language>__<profile-id>.png`로 확장합니다.

기기·언어 값은 각 feature의 `commonTest/ScreenshotCapture.kt`에 Kotlin으로 직접 선언합니다.
언어·프로필 ID를 바꾸면 세 워크플로의 Bash·jq 기대 파일명 규칙도 함께 수정합니다. 이 규칙은 테스트 설정이나 코드 생성 입력이 아닙니다.
Golden 수집·비교 결과·댓글 처리는 기존 GitHub Actions 워크플로 안에서 수행합니다. 별도 실행 스크립트는 없습니다.

작성 절차는 [스크린샷 작성 스킬](../../.agents/skills/caro-screenshot-testing/SKILL.md), 규칙은
[개발자 지침](../../docs/development/screenshot-testing.md), CI·Golden·게시는 [스크린샷 CI](../../docs/ci/screenshot-test.md)를 따릅니다.
PNG·보고서는 빌드 산출물이며 커밋하지 않습니다.
