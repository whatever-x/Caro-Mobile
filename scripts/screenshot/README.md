# 스크린샷 시나리오 목록

[scenarios.json](scenarios.json)은 Android/iOS 공통 시나리오 ID를, [scenarios.android.json](scenarios.android.json)은 Android 전용 시나리오 ID를 feature 모듈별로 관리합니다. CI는 플랫폼별 목록을 합쳐 생성된 PNG 이름과 비교 결과를 검증합니다.

## 등록 형식

```json
{
  "home": ["home.home.with_deck"],
  "login": ["login.login.initial"]
}
```

키는 feature 모듈명이고, 배열에는 해당 모듈의 캡처 테스트가 사용하는 시나리오 ID를 작성합니다.

## 수정 규칙

- 캡처 테스트를 추가·삭제하면 해당 플랫폼의 목록도 함께 수정합니다. 공통 목록과 Android 전용 목록에 같은 ID를 중복 등록하지 않습니다.
- ID는 영문·숫자로 시작하고, 영문·숫자·점·밑줄·하이픈만 사용합니다.
- `_actual`·`_compare` 접미사는 Roborazzi가 사용하므로 ID에 넣지 않습니다.
- 생성된 이미지와 보고서는 빌드 산출물이며 Git에 커밋하지 않습니다.

목록 결합과 입력 검증은 `python3 scripts/screenshot/test-resolve-scenarios.py`로 확인합니다.
