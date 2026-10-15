# MVI와 UI 작성

[전체 아키텍처](../../ARCHITECTURE.md)의 상세 지침입니다.
이 문서의 주제에 해당하는 코드를 구현·변경·리뷰하기 전에 읽습니다.
공통 작업 규칙은 루트 [AGENTS.md](../../AGENTS.md)를 따릅니다. 빌드·테스트 지침은 [문서 탐색 목록](../index.md)에서 선택합니다.

ViewModel은 `BaseViewModel<State, Intent, SideEffect>`를 상속합니다.
feature는 다음 MVI 구조를 따릅니다.

```
feature/<name>/
|-- route/
|   |-- <Name>Route.kt          # Composable 진입점(상태 수집·부수효과 처리)
|-- <Name>Screen.kt             # 순수 UI Composable(상태·Intent 콜백 전달받음)
|-- <Name>ViewModel.kt          # BaseViewModel 상속, feature의 DI 모듈에 등록
|-- di/
|   |-- <Name>Module.kt         # viewModel/single 바인딩을 선언하는 Koin `module { }`
|-- mvi/
    |-- <Name>Intent.kt         # 사용자 동작을 정의하는 sealed interface
    |-- <Name>State.kt          # UI 상태를 나타내는 data class
    |-- <Name>SideEffect.kt     # 일회성 이벤트를 정의하는 sealed interface
```

주요 ViewModel 도우미 함수:

- `reduce { copy(...) }` — 상태 변경
- `postSideEffect(effect)` — 일회성 부수효과 전달
- `launch { }` — 예외 처리가 포함된 코루틴 스코프에서 실행

단순 API 작업이 하나의 Repository 호출에 대응하면 feature의 ViewModel은 해당 Repository를 직접 호출할 수 있습니다.
단일 Repository 호출을 감싸기 위한 feature UseCase는 추가하지 않습니다.
다음 작업 중 하나 이상을 수행할 때만 UseCase를 추가합니다.

- 재사용할 비즈니스 규칙을 구현합니다.
- 여러 Repository 호출을 조율합니다.
- 단순 UI 매핑을 넘어 의미 있는 데이터 변환을 수행합니다.

로컬 UI 입력을 검증하는 도우미는 해당 feature 가까이에 둡니다.
`NicknameValidator`처럼 수행하는 동작에 맞게 이름을 짓습니다.
UI 입력 검증 도우미에 API UseCase 이름을 붙이지 않습니다.

Repository 계약은 도메인을 나타내는 기본 타입이나 모델을 노출합니다.
Repository는 한 화면에 데이터를 전달하기 위해 만든 응답 래퍼를 노출하지 않습니다.
닉네임 사용 가능 여부 조회는 `Boolean`을 반환합니다.
단, 앱이 백엔드의 구조화된 사유 데이터를 사용해야 하면 `Boolean` 반환 규칙의 예외로 합니다.

## UI 문자열

사용자에게 보이는 Compose 텍스트는 Compose resources(`stringResource(Res.string...)`)를 사용합니다.
접근성 라벨에도 Compose resources를 사용합니다.
Screen·Route·컴포넌트에서 UI 문자열을 하드코딩하지 않습니다.
단, 디버깅에만 사용하는 임시 UI는 문자열을 하드코딩할 수 있습니다.

## 상수와 컴포넌트 배치

상수 위치는 사용 범위로 결정합니다.

- 한 화면에서만 쓰는 UI 값은 해당 Screen 파일의 `private const val` 또는 `private val`로 둡니다.
- feature 내부의 ViewModel·State·Test 등에서 공유하는 입력 제한·정책 값은 `internal object XxxLimits`로 분리합니다. 이 객체는 해당 feature 패키지에 둡니다.
- 공유 제한 값의 예시는 `DeckInputLimits`입니다.
- 서버 정책이나 도메인 규칙에 가까운 값은 domain/model 또는 repository 계층에서 관리합니다.

Screen 전용의 작은 `private` Composable은 같은 Screen 파일에 둡니다.
다음 조건 중 하나에 해당하는 UI 조각만 feature 내부의 `components/` 패키지로 분리합니다.

- Screen 파일이 커집니다.
- 다른 화면에서 UI 조각을 재사용할 여지가 있습니다.

현재 feature(profile, deck 등)는 한 파일 유지를 기본으로 합니다.
`components/`를 도입하면 기존 화면도 같은 구조로 정리합니다.
