# 적용 예시 — PR #65 (프로필 생성 화면)

`caro-compose-review`를 실제 머지된 PR #65(`[Feat] 프로필 생성 화면 및 회원가입 완료 API 연동`)에
직접 적용한 기록이다. 스킬의 "사용 방법"·체크리스트·출력 형식을 그대로 따랐다.

## 1. 대상 확보

```bash
# 머지 커밋 기준 변경 파일 목록 (PR #65 = de900f1)
git diff-tree --no-commit-id --name-only -r de900f1
# 리뷰 대상: feature/profile/ 의 변경된 *.kt 만 추려서 점검
```

점검한 파일: `CreateProfileScreen.kt`, `CreateProfileViewModel.kt`, `CreateProfileRoute.kt`,
`NicknameValidator.kt`, `mvi/CreateProfile{State,Intent,SideEffect}.kt`, `di/ProfileModule.kt`.

## 2. Navigation3 4단계 등록 grep (체크리스트 H)

```bash
grep -rn 'CreateProfileEntry' core/navigator/ --include='*.kt'
grep -rn 'subclass(CreateProfileEntry' composeApp/ --include='*.kt'
grep -rn 'navigation<CreateProfileEntry>' composeApp/ --include='*.kt'
```

```text
① core/navigator/.../entries/CreateProfileEntry.kt:7: data object CreateProfileEntry : NavKey
② composeApp/.../CaroApp.kt:44:        subclass(CreateProfileEntry::class, CreateProfileEntry.serializer())
③ composeApp/.../di/NavigationModule.kt:36: navigation<CreateProfileEntry> {
```

셋 다 매치 → 등록 누락 없음(통과). ④ Route/Screen/ViewModel/MVI도 존재.

## 3. 결과

```text
## caro-compose-review 결과

### 🟡 Should-fix
- `feature/profile/di/ProfileModule.kt:13` — `viewModel<CreateProfileViewModel> { CreateProfileViewModel(get(), get(), get()) }`
  처럼 명시 생성자 블록을 씀. 모든 인자가 `get()`이라 단축 DSL `viewModel<CreateProfileViewModel>()`로 줄일 수 있다.
  같은 파일이 `single`은 `org.koin.plugin.module.dsl`(단축), `viewModel`은 `org.koin.core.module.dsl`(평범)을 섞어 import.
  근거: _conventions Koin DSL / 체크리스트 I.
- `feature/profile/CreateProfileViewModel.kt:48-52` — 닉네임 중복 확인 실패 시 `NicknameValidationResult.Valid`로 폴백.
  서버 에러(가용성 unknown)를 "사용 가능" 도메인 값으로 강제. `// TODO: 서버 에러 처리 UI 확정 시 폴백 제거` 주석 있음.
  근거: _conventions Flow/동시성 / 체크리스트 E(sentinel을 진짜 도메인 값처럼 쓰지 말 것). 의도된 임시값이나 추적 필요.
- `feature/profile/CreateProfileScreen.kt:42-45` — `PageHorizontalPadding = 28.dp`, `CtaButtonHeight = 56.dp`,
  `HeaderMinHeight = 17.dp` 매직 dp. 같은 화면의 `NicknameField`는 `CaroTheme.spacing.xl/m`을 쓰는데 CTA만 raw dp라 불일치.
  근거: _conventions 디자인 토큰 / 체크리스트 F.
- `feature/profile/CreateProfileRoute.kt` — Route가 모듈 루트에 있음. 컨벤션·`feature/home`은 `route/<Name>Route.kt`.
  근거: _conventions 파일 레이아웃 / 체크리스트 A.
- `feature/profile/src/commonTest` 비어 있음 — debounce·검증·에러 폴백 등 비자명 로직이 있는 ViewModel에 테스트 없음.
  `caro.kover`가 feature에 라인 50% 최소를 강제하므로 커버리지 게이트 위험. 근거: 체크리스트 K.

### 🟢 Nit
- `CtaButton`을 Box+clickable로 직접 구성(현재 designsystem에 CaroButton 부재 — 수용 가능, 추후 컴포넌트화 후보).
- 아이콘 `size(24.dp)`/`size(16.dp)`는 raw지만 드로어블 치수(ic_arrow_left_24/ic_renew_16)와 일치.

### 통과
- B. Route/Screen 분리: Screen이 `internal fun(state, onIntent)` 순수 UI, Route가 상태수집 + sideEffect collect + emit 담당.
- C. 상태 작성: Screen 완전 hoisted, 비즈니스 상태는 ViewModel.
- E(부분). suspendRunCatching로 CancellationException 재전파, repository는 suspend만 노출.
- G. Compose resources: 모든 노출 텍스트·contentDescription이 `stringResource`. 하드코딩 문자열 없음.
- H. Navigation3 4단계: 위 grep으로 ①②③ + ④ 전부 확인.
- J. 멀티플랫폼: commonMain, 시그니처에 플랫폼 타입 없음.
```

## 메모

- 위 발견은 **컨벤션 정합성**만 다룬다. 일반 버그·정확성은 `code-review:code-review`와 함께 본다(대체 아님).
- 스킬은 코드를 고치지 않고 지적·근거 제시까지가 기본. 실제 수정은 사용자가 요청할 때.
