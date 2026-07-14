# 일일학습 중단 다이얼로그 진행률 표시 설계

## 목표

일일학습과 전체학습이 공유하는 학습 중단 다이얼로그에서, 일일학습에만 완료한 카드 진행률을 `평가한 카드 {progress} / {totalCount}` 형식으로 표시한다. 전체학습 다이얼로그의 현재 내용과 레이아웃은 유지한다.

## 상태와 데이터 흐름

- `LearningViewModel`이 알고 있는 `LearningMode`를 `LearningState`의 화면 상태로 노출한다.
- `LearningScreen`은 `LearningMode.DAILY`일 때 `state.progress`와 `state.totalCount`를 `LearningStopDialog`에 전달한다.
- `state.progress`는 `LearningTopBar`가 현재 카드 번호를 계산할 때 사용하는 동일한 진행 상태이며, 다이얼로그에서는 이미 평가를 마친 카드 수로 그대로 표시한다.
- 전체학습에서는 평가 진행률 인자를 전달하지 않아 해당 UI를 렌더링하지 않는다.

## UI

- 기존 제목, 저장 안내 문구, `계속 학습하기`, `중단하기` 버튼은 유지한다.
- 일일학습에서는 안내 문구와 버튼 영역 사이에 한 행을 추가한다.
- 행의 왼쪽에는 리소스 문자열 `평가한 카드`, 오른쪽에는 `{progress} / {totalCount}`를 표시한다.
- 기존 `CaroDialog`, `CaroTheme` 색상·타이포그래피·간격 토큰을 재사용한다.
- 한국어와 기본 영어 Compose 리소스를 함께 추가한다.

## 컴포넌트 경계

- `LearningStopDialog`는 선택적인 평가 진행률을 받는다. 값이 없으면 관련 행과 여백을 모두 생략한다.
- 학습 모드 분기는 화면 호출부에서 수행해 다이얼로그가 `LearningMode` 자체에 의존하지 않도록 한다.
- 별도의 범용 디자인시스템 컴포넌트는 만들지 않는다. 현재 학습 기능에만 필요한 작은 UI이므로 기존 feature 컴포넌트 안에 유지한다.

## 테스트와 검증

- 일일학습 ViewModel 초기 상태가 `LearningMode.DAILY`를 화면 상태에 노출하는지 테스트한다.
- 전체학습 ViewModel 초기 상태가 `LearningMode.ALL`을 유지하는지 테스트한다.
- 학습 feature 테스트와 Spotless 검사를 실행한다.
- Preview에서 일일학습 진행률이 있는 다이얼로그와 전체학습 기본 다이얼로그를 각각 확인할 수 있게 한다.

## 범위 제외

- 중단 시 학습 데이터 저장 정책 변경
- `progress` 계산 규칙 변경
- 전체학습 다이얼로그 문구 또는 동작 변경
- 현재 작업 중인 카드별 학습시간 측정 변경
