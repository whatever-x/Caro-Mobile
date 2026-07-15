# ConfirmStop 평가 제출 설계

## 목적

일일 학습(`LearningMode.DAILY`) 도중 사용자가 중단을 확인하면, 현재 카드 이전까지 평가가 완료된 카드들의 평가를 서버에 제출한 뒤 학습 화면을 닫는다.

## 범위

- `LearningIntent.ConfirmStop` 수신 시 `LearningState.evaluations`에 누적된 평가만 제출한다.
- 현재 표시 중이지만 아직 평가하지 않은 카드는 제출 대상에 포함하지 않는다.
- `DAILY` 모드이고 누적 평가가 한 건 이상일 때만 `StudySessionRepository.submit(sessionId, evaluations)`을 호출한다.
- 제출 중에는 `isSubmitting`을 `true`로 유지하고, 성공하면 `false`로 되돌린 뒤 `LearningSideEffect.NavigateBack`을 방출한다.
- 누적 평가가 없거나 `LearningMode.ALL`이면 서버 호출 없이 즉시 `NavigateBack`을 방출한다.
- `Close` 인텐트와 마지막 카드 평가 시의 기존 동작은 변경하지 않는다.

## 오류 처리

제출이 실패하면 `BaseViewModel`의 기존 예외 처리 경로가 `isSubmitting`을 해제하고 오류 메시지를 상태에 기록한다. 이 경우 `NavigateBack`은 방출하지 않아 사용자가 평가 데이터를 잃지 않은 채 화면에 머무르게 한다.

## 구현 구조

`LearningViewModel.handleIntent`에서 `ConfirmStop`을 전용 suspend 함수로 전달한다. 함수는 모드와 누적 평가 여부를 검사한 뒤 필요한 경우 저장소 제출을 기다리고, 성공한 경우에만 화면 닫기 side effect를 방출한다. 이번 변경을 위해 새로운 상태나 repository API는 추가하지 않는다.

## 테스트

ViewModel 단위 테스트로 다음 계약을 검증한다.

1. `DAILY`에서 일부 카드를 평가한 뒤 중단하면 해당 평가 목록과 세션 ID가 제출되고, 이후 `NavigateBack`이 방출된다.
2. `DAILY`에서 평가 없이 중단하면 제출하지 않고 `NavigateBack`이 방출된다.
3. `ALL`에서 평가 후 중단하면 제출하지 않고 `NavigateBack`이 방출된다.

테스트는 먼저 기존 구현에서 실패하는 것을 확인한 뒤 최소 구현으로 통과시킨다.
