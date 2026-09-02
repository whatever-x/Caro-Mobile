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

```kotlin
SwipeBenchmarkScenario(
    mode = SwipeBenchmarkMode.FREE,
    inputs =
        listOf(
            SwipeInput(
                horizontalDistanceRatio = 0.25f,
                verticalDistanceRatio = -0.2f,
            ),
        ),
)
```

## 실행

```shell
./gradlew :benchmark:connectedBenchmarkAndroidTest
```

기본값은 테스트당 5회, 각 회차당 스와이프 6회입니다. 실행 인자로 조정할 수 있습니다.

```shell
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.benchmarkIterations=10 \
  -Pandroid.testInstrumentationRunnerArguments.swipesPerIteration=12 \
  -Pandroid.testInstrumentationRunnerArguments.resetSettleMillis=200
```

특정 시나리오만 실행할 수도 있습니다.

```shell
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.whatever.caro.benchmark.swipe.SwipeGestureBenchmark#freeSwipeFrameTiming
```

JSON과 Perfetto trace는 `benchmark/build/outputs/connected_android_test_additional_output`에 생성됩니다.
