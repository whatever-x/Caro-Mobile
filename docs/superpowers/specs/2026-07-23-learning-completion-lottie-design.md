# Learning Completion Lottie Design

## Goal

학습 완료 화면이 표시될 때 체크 Lottie 애니메이션을 처음부터 끝까지 한 번 재생하고, 재생이 끝나면 마지막 프레임을 유지한다.

## Current State

- `LearningCompletion`은 Compose resources의 `files/lottie_check_pop.json`을 `LottieCompositionSpec.JsonString`으로 읽는다.
- 현재 painter는 `Compottie.IterateForever`를 사용해 요구사항과 다르게 무한 반복하도록 설정되어 있다.
- 해당 변경은 Android `compileAndroidMain`에서 컴파일되지만, 반복 횟수와 비대화형 렌더링 방식이 완료 화면의 계약을 명확히 표현하지 않는다.

## Design

`LearningCompletion` 내부에서 composition을 기존 방식으로 비동기 로드한다. 비대화형 애니메이션이므로 Compottie 공식 문서의 기본 사용법에 맞춰 Compose `Image`와 `rememberLottiePainter`를 사용한다. Painter에는 `iterations = 1`을 명시해 완료 화면이 composition에 새로 들어올 때마다 한 번만 재생되도록 한다.

화면 크기, `ContentScale.Crop`, `contentDescription = null`, JSON 파일 위치와 완료 화면 레이아웃은 변경하지 않는다. ViewModel이나 MVI 상태에는 애니메이션 진행률을 추가하지 않는다. 재생은 화면에만 속한 일시적 UI 동작이므로 `LottieAnimatable`과 `LaunchedEffect`도 도입하지 않는다.

## Data and Lifecycle

1. 학습 상태가 완료로 전환되면 `LearningCompletion`이 composition에 들어온다.
2. Compose resource JSON이 파싱되어 `LottieComposition`이 준비된다.
3. `rememberLottiePainter`가 진행률 0에서 재생을 시작한다.
4. 한 번 재생한 뒤 진행률 1에서 멈춘다.
5. 완료 화면이 제거되었다가 새로운 학습 완료로 다시 생성되면 새 painter 수명에서 다시 한 번 재생한다.

## Error Handling

Composition 로딩 중에는 painter가 그려지지 않으며 기존 완료 화면의 나머지 콘텐츠는 정상 표시된다. 이번 범위에서는 별도 오류 UI나 정적 아이콘 fallback을 추가하지 않는다.

## Verification

- 반복 횟수가 정확히 1로 고정됐음을 작은 소스 계약 테스트로 검증한다.
- Learning 모듈의 Android host 테스트를 실행한다.
- Android와 iOS Simulator 타깃 컴파일로 공통 코드 호환성을 확인한다.
- Spotless 검사로 포맷을 검증한다.

## Out of Scope

- Lottie JSON 디자인 변경
- 애니메이션 속도, 구간, 색상 동적 변경
- 앱 세션 전체에서 최초 한 번만 재생하는 영속 상태
- 완료 화면 레이아웃 및 통계 UI 변경
