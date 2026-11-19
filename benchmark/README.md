# Macrobenchmark

앱의 주요 동작을 비디버그 target에서 반복 측정합니다. 현재 첫 시나리오는 `core:ui`의 스와이프이며, 스크롤이나 화면 전환도 같은 모듈에 추가할 수 있습니다.

## 구성

- `benchmark/common`: 반복 횟수와 실행 대상처럼 모든 시나리오에서 사용하는 설정
- `benchmark/{scenario}`: 입력, 화면 제어, 결과 검증과 측정 코드
- `benchmark-target/{scenario}`: 제품 코드를 실행하는 시나리오별 화면

시나리오 전용 값은 해당 패키지의 data class에 두고, 입력 계산과 실행은 함수로 분리합니다.

## 실행

연결된 기기에서 전체 벤치마크를 실행합니다.

```shell
./gradlew :benchmark:connectedBenchmarkAndroidTest
```

기본값은 테스트당 5회, 회차당 스와이프 6회입니다.

```shell
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.benchmarkIterations=10 \
  -Pandroid.testInstrumentationRunnerArguments.swipesPerIteration=12
```

## 스와이프 시나리오

| 테스트 | 측정 동작 |
| --- | --- |
| `freeSwipeFrameTiming` | 자유 방향으로 짧게 밀고 복귀 |
| `lockedSwipeFrameTiming` | 방향 고정으로 짧게 밀고 복귀 |
| `freeSwipeExitFrameTiming` | 자유 방향으로 카드를 넘김 |
| `lockedSwipeExitFrameTiming` | 방향 고정으로 카드를 넘김 |

입력은 카드 크기 대비 비율로 정의합니다. 각 입력 뒤 결과와 방향, 입력 순서, 카드 번호와 다음 카드의 준비 상태를 확인합니다.

## 시나리오 추가

1. `benchmark-target`에 제품 코드를 실행할 화면을 추가합니다.
2. `benchmark`에 시나리오 패키지를 만들고 입력, driver와 테스트를 둡니다.
3. 프레임마다 바뀌는 값 대신 준비 상태와 완료 결과만 target에서 노출합니다.
4. 동작 조건이 다른 결과는 테스트를 나눠 측정합니다.

## 결과

JSON과 Perfetto trace는 `benchmark/build/outputs/connected_android_test_additional_output`에 생성됩니다. 결과와 그래프는 저장소에 쌓지 않고 PR이나 별도 성능 문서에 기록합니다.

측정 조건이 같을 때만 이전 결과와 비교합니다. 에뮬레이터 실행은 동작 확인에 사용하고, 최종 성능은 실기기에서 확인합니다.
