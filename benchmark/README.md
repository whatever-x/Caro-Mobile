# Macrobenchmark

앱의 주요 사용자 동작을 실제 화면과 분리된 비디버그 target에서 반복 측정합니다.
현재 첫 시나리오는 `core:ui`의 스와이프 제스처이며, 이후 스크롤, 화면 전환, 시작 성능 같은 시나리오를 같은 모듈에 추가할 수 있습니다.

## 모듈 구성

```text
benchmark
└── src/main/kotlin/com/whatever/caro/benchmark
    ├── common
    │   ├── BenchmarkRunConfig.kt
    │   └── BenchmarkTarget.kt
    └── swipe
        ├── SwipeBenchmarkConfig.kt
        ├── SwipeBenchmarkDriver.kt
        ├── SwipeBenchmarkScenario.kt
        └── SwipeGestureBenchmark.kt

benchmark-target
└── src/main/kotlin/com/whatever/caro/benchmark/target
    └── swipe
        └── SwipeBenchmarkActivity.kt
```

- `common`: 시나리오와 무관한 반복 횟수, 실행 대상 정보
- 시나리오 패키지: 입력 데이터, 화면 탐색, 동작 실행, 측정 테스트
- `benchmark-target`: 실제 제품 코드를 렌더링하는 비디버그 화면

측정 결과와 그래프는 코드에 누적하지 않고 PR이나 별도 성능 문서에 기록합니다.

## 시나리오 추가 원칙

1. `benchmark-target`에 시나리오 전용 Activity를 추가합니다.
2. `benchmark`에 시나리오 이름의 패키지를 만들고 설정, 입력, driver, 테스트를 둡니다.
3. 반복 횟수와 앱 실행 대상은 `common`의 `BenchmarkRunConfig`, `BenchmarkTarget`을 재사용합니다.
4. 시나리오에만 필요한 값은 `common`에 올리지 않고 해당 패키지의 data class에 둡니다.
5. 입력 목록은 data class로 정의하고, 좌표 계산과 실행은 함수로 분리합니다.

테스트 함수 이름은 이전 결과와 비교할 때 식별자로 사용되므로 측정 의미가 같다면 유지합니다.

## 스와이프 시나리오

스와이프 입력은 화면 좌표 대신 카드 크기 대비 비율로 정의합니다. 기기 해상도가 달라져도 같은 비율의 제스처를 실행할 수 있습니다.
입력 성공 여부는 `UiDevice.swipe()` 반환값만으로 판단하지 않습니다. target이 노출하는 안정적인 종료 이벤트를 기다리고 결과와 방향, 몇 번째 입력인지 나타내는 순번, 몇 번째 카드인지 나타내는 번호를 확인한 뒤 카드가 최초 위치에 준비됐는지 검증합니다.

```kotlin
SwipeBenchmarkScenario(
    mode = SwipeBenchmarkMode.FREE,
    expectedResult = SwipeTerminalResult.RESET,
    inputs =
        listOf(
            SwipeInput(
                horizontalDistanceRatio = 0.25f,
                verticalDistanceRatio = 0f,
                expectedDirection = SwipeDirection.RIGHT,
            ),
        ),
)
```

### reset과 exit 측정

두 결과는 애니메이션 길이와 화면 상태가 다르므로 서로 다른 테스트로 측정합니다.

- `freeSwipeFrameTiming`, `lockedSwipeFrameTiming`: 기존과 같은 짧은 입력을 사용합니다. 280 x 360 dp 카드에서 가로 `0.25`, 세로 `0.2` 비율로 입력해 완료 기준 미만의 reset을 측정합니다. 각 입력 뒤 카드는 최초 중심과 크기로 돌아와야 하며 카드 세대가 바뀌면 실패합니다.
- `freeSwipeExitFrameTiming`, `lockedSwipeExitFrameTiming`: 가로 `0.55`, 세로 `0.45` 비율의 긴 입력으로 exit를 측정합니다. 완료 콜백은 기대 방향으로 정확히 한 번 발생해야 하며, target은 종료된 카드를 새 세대의 원점 카드로 교체합니다.

기본 제스처 설정은 민감도 `1.28`, 완료 기준 `88.dp`입니다. `detectDragGestures`가 touch slop 이후 이동량을 전달하므로 기존 가로 `0.25`, 세로 `0.2` 입력은 일반적으로 reset 경로에 해당합니다. 실제 기기별 입력 해석은 매 입력의 종료 결과 검증으로 확인합니다.

관찰용 semantics는 드래그 위치나 진행률을 노출하지 않습니다. 방향이 활성화되거나 제스처가 종료될 때만 준비 여부, 입력 순번, 결과, 방향과 카드 번호를 갱신하므로 프레임마다 관찰 UI가 리컴포지션되지 않습니다.

## 실행

```shell
./gradlew :benchmark:connectedBenchmarkAndroidTest
```

기본값은 테스트당 5회, 각 회차당 스와이프 6회입니다. 실행 인자로 조정할 수 있습니다.

```shell
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.benchmarkIterations=10 \
  -Pandroid.testInstrumentationRunnerArguments.swipesPerIteration=12 \
  -Pandroid.testInstrumentationRunnerArguments.gestureTimeoutMillis=5000
```

특정 시나리오만 실행할 수도 있습니다.

```shell
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.whatever.caro.benchmark.swipe.SwipeGestureBenchmark#freeSwipeFrameTiming
```

JSON과 Perfetto trace는 `benchmark/build/outputs/connected_android_test_additional_output`에 생성됩니다.
이번 검증에서는 고정 200 ms 대기를 상태 기반 대기로 바꾸고 관찰 표식과 카드 교체를 추가했으므로 기존 그래프 수치와 직접 비교하지 않습니다. 새 기준선을 만든 뒤 reset은 reset 테스트끼리, exit는 exit 테스트끼리 비교합니다. 에뮬레이터 스모크 실행은 동작 계약 확인용이며 성능 개선 근거로 해석하지 않습니다.

## 스와이프 입력 확장

1. `benchmark-target` 화면에 프레임 단위 값이 아닌 안정적인 준비 표식과 종료 이벤트를 노출합니다.
2. 시나리오의 `SwipeInput`에 기대 방향을, `SwipeBenchmarkScenario`에 기대 종료 결과를 명시합니다.
3. driver에서 입력 전 원점, 입력 뒤 입력 순번 증가, 종료 결과와 다음 입력의 준비 상태를 검증합니다.
4. reset과 exit처럼 렌더링 계약이 다른 결과는 테스트 함수를 분리해 결과 파일이 섞이지 않게 합니다.
