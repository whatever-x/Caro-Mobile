# `.claude/skills` — Caro-Mobile 에이전트 스킬

이 디렉토리의 스킬은 Claude/Codex 등 에이전트가 코딩·리뷰 시 자동 발동(description 트리거)하거나
직접 호출(`/<skill-name>`)할 수 있다. YAML frontmatter만 Claude 전용이며, 절차·규칙은 에이전트 중립적이다.

## 구성

### 컨벤션 단일 출처
- **`_conventions`** — Caro 아키텍처 규칙 요약(MVI·BaseViewModel·Koin·Navigation3·Compose resources·
  expect/actual·Kotest). 다른 스킬이 "우리 컨벤션은?"의 근거로 삼는다. 권위 원문은 루트 `CLAUDE.md`.

### Compose / Kotlin / KMP 스킬 (벤더링 + 각색)
출처: [`chrisbanes/skills`](https://github.com/chrisbanes/skills) (Apache-2.0). 원문 기술 내용을 유지하되
예시 코드를 Caro 컨벤션(`CaroTheme`, `BaseViewModel`, Koin 단축 DSL, `stringResource(Res.string)`,
`NavKey`, `expect/actual`, Kotest)으로 각색하고 Android 전용 표현을 CMP-안전하게 다듬었다.

| 스킬 | 한 줄 |
|---|---|
| `kotlin-multiplatform-expect-actual` | expect/actual·인터페이스 경계, commonMain에 플랫폼 타입 금지 |
| `kotlin-coroutines-structured-concurrency` | 스코프 소유, suspend API, 취소 재전파, runBlocking 회피 |
| `kotlin-flow-state-event-modeling` | StateFlow vs Channel(sideEffect), update{}, sentinel 회피 |
| `compose-state-authoring` | remember/mutableStateOf, @ReadOnlyComposable |
| `compose-state-hoisting` | 상태를 로컬/holder/ViewModel 중 어디에 둘지 |
| `compose-state-holder-ui-split` | Route(배선) / Screen(순수 UI) 분리 |
| `compose-side-effects` | LaunchedEffect/DisposableEffect/rememberCoroutineScope, effect key |
| `compose-recomposition-performance` | recomposition 성능 라우터(안정성/지연읽기/back-writing) |
| `compose-slot-api-pattern` | 재사용 컴포넌트 슬롯 API(디자인시스템) |
| `compose-ui-testing-patterns` | ViewModel/UI 테스트(Kotest+Mokkery+Turbine로 각색) |

### 리뷰
- **`caro-compose-review`** — PR/diff를 위 컨벤션 체크리스트로 리뷰. 직접 호출(`/caro-compose-review`) +
  "컴포즈 리뷰" 등 트리거. 일반 버그 리뷰(`code-review:code-review`)와 **보완** 관계.

### 기타
- `swagger-sync` — Swagger → Kotlin DTO/API 동기화(별도 출처).

## 멀티플랫폼 적합성

- **chrisbanes/skills**: KMP+Compose Multiplatform에 적합하여 벤더링.
- **android/skills**(AGP9·CameraX·R8·Play·Wear·XR·edge-to-edge·profilers·androidx Navigation3 등):
  Android 전용이라 KMP+iOS 앱에 부적합 → **벤더링 제외**. 필요 시 원문 참조.
- 제외/보류한 chrisbanes 스킬: `compose-stability-diagnostics`, `compose-state-deferred-reads`,
  `compose-modifier-and-layout-style`, `compose-animations`, `kotlin-types-value-class`,
  `compose-focus-navigation`, `shepherd`. 필요해지면 아래 절차로 추가.

## 상류 변경 재동기화 절차

1. 원문 fetch: `https://raw.githubusercontent.com/chrisbanes/skills/main/skills/<name>/SKILL.md`
2. 우리 버전과 diff하여 새 규칙/예시 변경 파악.
3. frontmatter는 우리 형식 유지(`name` + 한국어 트리거 description), 본문 예시는 Caro 컨벤션으로 재각색.
4. Android 전용 표현(`Activity`, Espresso/JUnit, Hilt `@Inject`)은 CMP-안전/Caro 스택으로 치환.
5. `_conventions`·`CLAUDE.md`로의 상호참조 링크 유지.

## 라이선스

벤더링한 Compose/Kotlin/KMP 스킬은 chrisbanes/skills의 Apache License 2.0을 따른다.
각 SKILL.md 상단에 출처·라이선스를 표기했다.
