# 스크린샷 캡처 계약

[scenarios.json](scenarios.json)은 feature 모듈별 기대 논리 ID 목록입니다. 실행 코드는 각 feature의 `commonTest` 시나리오와
Android/iOS 래퍼에 있습니다. 목록을 고쳐도 테스트가 생성되거나 실행되지는 않습니다.

```json
{
  "home": ["home.home.with_deck"],
  "login": ["login.login.initial"]
}
```

[capture-config.json](capture-config.json)은 ko/en과 플랫폼 프로필·variant·캡처 bounds의 단일 원본입니다.
기대 파일명은 `<scenario-id>__<language>__<profile-id>.png`로 확장합니다. ID는 영문·숫자로 시작하며 영문·숫자·점·밑줄·하이픈을
사용합니다. `__`와 `_actual`·`_compare`·`_before` 접미사는 출력 identity/Roborazzi용으로 예약합니다. ID는 모듈 전체에서 고유해야 합니다.

[pipeline.py](pipeline.py)는 확장·PNG·Golden·결과 수집·게시 검증과 플랫폼 댓글 생성을 담당합니다.
의존성은 Python 3 표준 라이브러리이며, 실제 동작 fixture는 [test_pipeline.py](test_pipeline.py)입니다.

```bash
# 저장소 루트에서 실행
python3 scripts/screenshot/pipeline.py expand --platform android
python3 scripts/screenshot/pipeline.py expand --platform ios
python3 scripts/screenshot/test_pipeline.py
```

개발과 캡처 API는 [스크린샷 테스트 개발](../../docs/development/screenshot-testing.md),
CI·exact-base Golden·v2 마이그레이션·게시 승인과 결과 확인은 [스크린샷 CI](../../docs/ci/screenshot-test.md)를 따릅니다.
PNG·보고서는 빌드 산출물이며 커밋하지 않습니다.
