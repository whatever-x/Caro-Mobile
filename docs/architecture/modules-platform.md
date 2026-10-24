# 모듈 경계와 플랫폼 코드

[전체 아키텍처](../../ARCHITECTURE.md)의 상세 지침입니다.
이 문서의 주제에 해당하는 코드를 구현·변경·리뷰하기 전에 읽습니다.
공통 작업 규칙은 루트 [AGENTS.md](../../AGENTS.md)를 따릅니다. 빌드·테스트 지침은 [문서 탐색 목록](../index.md)에서 선택합니다.

## 모듈 구조

패키지 네임스페이스는 `com.whatever.caro`입니다.
프로젝트의 주요 모듈과 역할은 다음과 같습니다.

| 모듈·구성 | 역할 |
| --- | --- |
| `:androidApp` | Android 앱 진입점 |
| `iosApp/` | iOS 진입점과 Xcode 프로젝트, KMP 프레임워크 연동 |
| `:composeApp` | `CaroApp`·내비게이션 호스트·DI 연결을 포함하는 공유 KMP 모듈. 앱 구성에 필요한 core와 모든 feature 모듈을 조립 |
| `:core:*` | 공용 인프라 모듈 묶음 |
| `:core:data` | Repository 계층 |
| `:core:datastore` | 로컬 설정·상태 저장 |
| `:core:model` | `User` 등의 데이터 모델 |
| `:core:designsystem` | 테마·디자인 토큰 |
| `:core:ui` | 공유 Composable 유틸리티 |
| `:core:viewmodel` | MVI 패턴의 BaseViewModel 추상화 |
| `:core:navigator` | 내비게이션 키·명령·디스패처 |
| `:core:remote` | Ktor HTTP 클라이언트(Android의 OkHttp, iOS의 Darwin)와 DataSource |
| `:core:analytics` | 분석 이벤트 수집의 공통 계약과 플랫폼별 Firebase 연동 |
| `:core:crashlytics` | 오류·크래시 수집의 공통 계약과 플랫폼별 Firebase 연동 |
| `:core:messaging` | 푸시 토큰·메시지 수신·알림 표시와 플랫폼별 Firebase 연동 |
| `:core:util` | 숫자 포맷 등의 공통 유틸리티와 플랫폼별 구현 |
| `:feature:*` | home·login 등의 기능별 화면·상태·ViewModel. `caro.feature` 플러그인으로 core 의존성을 자동 주입 |
| `build-logic/convention/` | 모듈 설정을 표준화하는 Gradle 컨벤션 플러그인 |

## 모듈 의존 관계

이 절은 프로젝트 모듈 간 직접 의존성을 설명합니다. 외부 라이브러리 의존성은 각 모듈의 `build.gradle.kts`를 확인합니다.
화살표는 의존성을 선언하는 모듈에서 의존 대상으로 향합니다.

### 앱 조립

```text
:androidApp → :composeApp, :core:messaging
:composeApp → 앱 구성에 필요한 core 모듈 + 모든 feature 모듈
```

`:composeApp`은 `:core:util`을 제외한 core 모듈에 직접 의존합니다. `:core:util`은 `:feature:home`의 의존성으로 연결됩니다.
`:core:analytics`·`:core:crashlytics`·`:core:messaging`과 `:feature:login`은 `api`, 나머지 프로젝트 모듈은 `implementation`으로 선언합니다.
등록 원본은 [composeApp의 빌드 설정](../../composeApp/build.gradle.kts)과 [Android 앱의 빌드 설정](../../androidApp/build.gradle.kts)입니다.
iOS는 Gradle 앱 모듈 대신 Xcode 프로젝트에서 공유 KMP 프레임워크를 연동합니다.

### core 내부 의존 관계

다음 표는 각 core 모듈의 직접 프로젝트 의존성을 모두 나타냅니다. `없음`은 다른 프로젝트 모듈에 직접 의존하지 않는다는 뜻입니다.

| 모듈 | 직접 의존하는 프로젝트 모듈 | 선언 방식 |
| --- | --- | --- |
| `:core:data` | `:core:remote`, `:core:datastore`, `:core:model` | `implementation` |
| `:core:remote` | `:core:model` | `implementation` |
| `:core:ui` | `:core:designsystem` | `implementation` |
| `:core:navigator` | `:core:model` | `api` |
| `:core:model` | 없음 | — |
| `:core:datastore` | 없음 | — |
| `:core:designsystem` | 없음 | — |
| `:core:viewmodel` | 없음 | — |
| `:core:analytics` | 없음 | — |
| `:core:crashlytics` | 없음 | — |
| `:core:messaging` | 없음 | — |
| `:core:util` | 없음 | — |

`api` 의존성은 호출 모듈에도 노출됩니다. `implementation` 의존성은 호출 모듈의 컴파일 의존성으로 자동 노출되지 않습니다.
예를 들어 `:core:navigator`를 사용하는 feature에서는 `:core:model` 타입을 사용할 수 있지만, `:core:data`를 사용한다는 이유로 `:core:remote`를 직접 사용할 수 있다고 가정하지 않습니다.

### feature와 core 의존 관계

모든 feature는 `caro.feature` 플러그인을 적용합니다. 플러그인은 `commonMain`에 다음 프로젝트 의존성을 `implementation`으로 추가합니다.

- `:core:designsystem`
- `:core:data`
- `:core:ui`
- `:core:viewmodel`
- `:core:navigator`

설정 원본은 [FeaturePlugin.kt](../../build-logic/convention/src/main/kotlin/FeaturePlugin.kt)입니다.
각 feature의 `build.gradle.kts`에서 직접 선언한 추가 프로젝트 의존성은 다음과 같습니다.

| feature 모듈 | 공통 설정 외 직접 추가한 프로젝트 의존성 |
| --- | --- |
| `:feature:card` | `:core:model` |
| `:feature:deck` | 없음 |
| `:feature:deck-detail` | `:core:model` |
| `:feature:home` | `:core:model`, `:core:util` |
| `:feature:learning` | `:core:model` |
| `:feature:login` | `:core:model` |
| `:feature:profile` | `:core:model` |
| `:feature:setting` | `:core:model` |
| `:feature:splash` | 없음 |

추가 의존성은 모두 `implementation`으로 선언합니다.
`:core:navigator`의 `:core:model` 노출(`api`)은 위 표의 직접 추가와 별개입니다.

feature 모듈끼리 직접 의존하지 않으며, core 모듈은 feature 구현에 의존하지 않습니다.
화면 간 연결은 `:core:navigator`의 키·명령을 경유하고, feature의 Route 등록과 DI 조립은 `:composeApp`에서 수행합니다.

## 플랫폼 진입점

Android는 [MainActivity](../../androidApp/src/main/kotlin/com/whatever/caro/MainActivity.kt)에서 UI를 시작하고, [CaroApplication](../../androidApp/src/main/kotlin/com/whatever/caro/CaroApplication.kt)에서 Koin을 초기화합니다.

iOS의 SwiftUI 진입점은 [iOSApp.swift](../../iosApp/iosApp/iOSApp.swift)입니다. `init()`에서 Swift의 `KoinKt.doInitKoin()`을 호출해 공유 Kotlin의 `initKoin()`을 실행합니다. 공유 DI 구성은 [의존성 주입 지침](dependency-injection.md)을 따릅니다.

iOS 프레임워크 연동과 서명 준비는 [개발 환경과 빌드](../development/build.md#ios-빌드)를 확인합니다.

## 플랫폼 코드

플랫폼 코드는 expect/actual 선언으로 분리합니다.
플랫폼 파일명은 `Foo.android.kt`와 `Foo.ios.kt`로 지정합니다.
예를 들어 `HttpClientEngineProvider`는 공통 인터페이스와 플랫폼별 구현을 갖습니다.
Android 구현은 OkHttp를 사용합니다.
iOS 구현은 Darwin을 사용합니다.
