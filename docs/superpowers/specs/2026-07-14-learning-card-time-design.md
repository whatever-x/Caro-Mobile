# Learning Card Time Design

## Goal

일일 학습 평가 API의 `timeMs`에 각 카드가 화면에 표시된 순간부터 사용자가 평가를 확정한 순간까지의 경과 시간을 기록한다.

## Root Cause

평가 버튼과 스와이프는 `LearningIntent.Evaluate(rating)`만 전달한다. `Evaluate.timeMs`의 기본값이 `0`이므로 ViewModel, repository, request DTO를 거쳐 API에도 항상 `0`이 전송된다. DTO 직렬화와 repository 매핑은 이미 `timeMs`를 보존하므로 측정값을 만들지 않는 학습 feature가 원인이다.

## Design

`LearningViewModel`이 현재 카드의 표시 시작 시각을 소유한다. 학습 카드 목록을 로드해 첫 카드를 노출할 때 단조 시계의 현재 값을 저장한다. 버튼 또는 스와이프로 평가하면 현재 시각과 시작 시각의 차이를 밀리초로 계산해 `StudyEvaluation.timeMs`에 기록한다. 다음 카드로 이동하는 즉시 시작 시각을 새 값으로 재설정한다.

측정은 시스템 벽시계 변경에 영향을 받지 않는 단조 시계를 사용한다. 시계 함수는 ViewModel 생성자에 기본값과 함께 주입해 프로덕션에서는 실제 경과 시간을 사용하고 테스트에서는 결정적인 값을 제공한다. API 입력이 아닌 내부 계산값이므로 `LearningIntent.Evaluate`에서는 `timeMs`를 제거한다.

밀리초 차이는 API 모델의 `Int` 범위로 제한한다. 시계 값이 비정상적으로 역행한 경우에는 음수가 전송되지 않도록 `0`으로 제한한다.

## Data Flow

1. 학습 세션 또는 전체 카드 로드가 끝나 현재 카드가 표시된다.
2. ViewModel이 해당 카드의 시작 시각을 저장한다.
3. 사용자가 버튼 또는 스와이프로 평가한다.
4. ViewModel이 카드별 경과 시간을 계산해 평가 목록에 누적한다.
5. 다음 카드가 있으면 카드 전환 시 새 시작 시각을 저장한다.
6. 일일 학습의 마지막 카드에서는 기존 흐름대로 누적 평가 목록을 API에 제출한다.

전체 학습도 같은 카드별 측정을 수행하지만 기존 정책대로 평가를 서버에 제출하지 않는다.

## Testing

- 첫 카드가 표시된 뒤 평가할 때 경과 시간이 해당 카드의 `StudyEvaluation.timeMs`에 기록되는지 검증한다.
- 다음 카드의 측정 시작점이 재설정되어 각 카드의 시간이 독립적으로 기록되는지 검증한다.
- repository가 `StudyEvaluation.timeMs`를 `EvaluatedCardRequest.timeMs`에 보존하는 회귀 테스트를 추가한다.
- feature 테스트와 formatting 검사를 실행한다.

## Scope

화면에 타이머를 표시하거나, 앱 백그라운드 시간을 제외하거나, 중단한 세션의 측정 시간을 복원하는 기능은 포함하지 않는다.
