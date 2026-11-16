# 테스트 안내

테스트를 작성·수정하거나 변경 사항을 검증할 때 읽습니다. 공통 검증 체크리스트는 루트 [AGENTS.md](../../AGENTS.md#변경-후-검증)를 따릅니다.

## 테스트 도구

| 도구 | 용도·규칙 |
| --- | --- |
| Kotest | `FunSpec`의 `init { }` 블록에 테스트 작성. Android 호스트 테스트는 JUnit5 사용 |
| Mokkery | 멀티플랫폼 모킹 |
| Turbine | Flow 테스트 |
| kotlinx-coroutines-test | 코루틴 테스트 |
| Koin Test | `KoinTest` 인터페이스와 `KoinExtension`으로 DI 테스트 |

설정 원본은 [테스트 컨벤션](../../build-logic/convention/src/main/kotlin/KmpTestPlugin.kt)입니다. 테스트 작성·리뷰에 적용할 스킬은 [스킬 목록](../../.agents/skills/index.md)에서 선택합니다.

## 소스셋과 실행 대상

| 소스 경로 | 역할 |
| --- | --- |
| `src/commonTest/kotlin` | 플랫폼 간 공유 테스트 |
| `src/androidHostTest/kotlin` | Android 호스트 전용 테스트 |
| `src/iosSimulatorArm64Test/kotlin` | ARM64 iOS 시뮬레이터 전용 테스트 |

실행할 수 있는 테스트 대상은 모듈의 플랫폼·플러그인 설정에 따라 다릅니다. iOS 시뮬레이터 테스트는 해당 macOS·Xcode 환경에서 실행합니다.

## 테스트 명령

저장소 루트에서 실행합니다. `<모듈 경로>`에는 `feature:home`, `core:data` 등 대상 모듈의 경로를 사용합니다.

```text
# 모듈의 Android 호스트 테스트
./gradlew :<모듈 경로>:testAndroidHostTest

# 모듈의 iOS 시뮬레이터 테스트
./gradlew :<모듈 경로>:iosSimulatorArm64Test

# 모듈의 전체 테스트
./gradlew :<모듈 경로>:allTests

# 모듈의 실행 가능한 태스크 확인
./gradlew :<모듈 경로>:tasks --all
```

```bash
# 전체 모듈 테스트
./gradlew allTests
```

변경과 관련된 모듈·플랫폼부터 검사합니다. 모든 모듈에 같은 테스트 태스크가 있다고 가정하지 않습니다.

## 커버리지

```text
# 모듈별 HTML 보고서
./gradlew :<모듈 경로>:koverHtmlReport
```

```bash
# 기존 루트 경로의 HTML 보고서 명령
./gradlew koverHtmlReport

# CI와 같은 대상 모듈의 XML 보고서·기준 검증
./gradlew koverAllXmlReport
./gradlew koverAllVerify
```

[커버리지 컨벤션](../../build-logic/convention/src/main/kotlin/KoverPlugin.kt)은 `:core:data`의 RepositoryImpl과 `:feature:*`의 ViewModel을 대상으로 합니다. 현재 Gradle 검증 하한은 라인 커버리지 50%입니다. PR 댓글의 80% 표시 기준과 구분합니다. 대상 집계는 [루트 빌드 설정](../../build.gradle.kts)을 확인합니다.

스크린샷의 공통 기준은 [스크린샷 테스트 규칙](screenshot-testing.md), 작성 절차와 예제는
[스크린샷 작성 스킬](../../.agents/skills/caro-screenshot-testing/SKILL.md)을 따릅니다.
스크린샷 비교·Golden·댓글 게시 확인은 [스크린샷 CI](../ci/screenshot-test.md), 일반 PR 검사 구성은 [CI 검사 안내](../ci/checks.md)를 따릅니다.
