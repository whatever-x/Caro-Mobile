# 스킬 탐색 목록

사용자 요청과 아래 적용 조건을 확인해 필요한 스킬을 선택합니다.
선택·사용 절차, 설치 방법, 문서 작성 규칙은 [에이전트 스킬 관리 지침](AGENTS.md)을 따릅니다.

## 작업별 스킬

| 작업 성격·적용 조건 | 읽을 문서 |
| --- | --- |
| Swagger/OpenAPI 변경을 Kotlin DTO·Ktorfit API에 반영하거나 동기화 PR을 생성할 때 | [swagger-sync/SKILL.md](swagger-sync/SKILL.md) |
| 프로젝트 컨벤션·모듈 구조·MVI·화면 추가 절차를 확인할 때 | [caro-conventions/SKILL.md](caro-conventions/SKILL.md) |
| Compose UI·MVI 코드의 PR·diff를 리뷰할 때 | [caro-compose-review/SKILL.md](caro-compose-review/SKILL.md) |
| `expect/actual`·플랫폼 분기·`commonMain` 경계를 설계하거나 검토할 때 | [kotlin-multiplatform-expect-actual/SKILL.md](kotlin-multiplatform-expect-actual/SKILL.md) |
| 코루틴 스코프·취소·수명·`runBlocking` 사용을 설계하거나 검토할 때 | [kotlin-coroutines-structured-concurrency/SKILL.md](kotlin-coroutines-structured-concurrency/SKILL.md) |
| `StateFlow`·`SharedFlow`·`Channel`로 상태와 이벤트를 모델링할 때 | [kotlin-flow-state-event-modeling/SKILL.md](kotlin-flow-state-event-modeling/SKILL.md) |
| `remember`·`mutableStateOf` 등 Compose 상태를 작성하거나 검토할 때 | [compose-state-authoring/SKILL.md](compose-state-authoring/SKILL.md) |
| UI 상태·로직의 소유 위치와 상태 끌어올리기 범위를 결정할 때 | [compose-state-hoisting/SKILL.md](compose-state-hoisting/SKILL.md) |
| Route·Screen을 나누거나 상태 수집·이벤트 연결과 UI 렌더링을 분리할 때 | [compose-state-holder-ui-split/SKILL.md](compose-state-holder-ui-split/SKILL.md) |
| `LaunchedEffect`·`DisposableEffect` 등 Compose 부수효과 API를 선택하거나 검토할 때 | [compose-side-effects/SKILL.md](compose-side-effects/SKILL.md) |
| 리컴포지션·안정성·Compose 성능을 점검할 때 | [compose-recomposition-performance/SKILL.md](compose-recomposition-performance/SKILL.md) |
| 슬롯 API를 활용한 재사용 UI·디자인 시스템 컴포넌트를 설계하거나 리뷰할 때 | [compose-slot-api-pattern/SKILL.md](compose-slot-api-pattern/SKILL.md) |
| Compose UI·상태홀더의 상태 분기·콜백·이벤트·부수효과 테스트를 작성하거나 리뷰할 때 | [compose-ui-testing-patterns/SKILL.md](compose-ui-testing-patterns/SKILL.md) |
| 스크린샷 시나리오·캡처 추가/수정/삭제 또는 Android/iOS·ko/en 캡처 범위를 점검할 때 | [caro-screenshot-testing/SKILL.md](caro-screenshot-testing/SKILL.md) |
