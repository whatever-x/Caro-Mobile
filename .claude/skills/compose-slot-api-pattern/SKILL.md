---
name: compose-slot-api-pattern
description: >
  caller마다 시각 영역이 달라지는 재사용 Compose 컴포넌트를 설계·리뷰하거나, primitive content 파라미터와
  boolean shape 플래그가 쌓일 때 사용한다. "슬롯 API", "재사용 컴포넌트", "title: String 파라미터",
  "showXxx 플래그", "디자인시스템 컴포넌트" 맥락에서 발동. Caro core:designsystem(CaroTextField 등) 설계에 적용.
---

# Compose: 슬롯 API 패턴

> 출처: chrisbanes/skills (Apache-2.0) · 벤더링 2026-06-09. 예시를 Caro 디자인시스템(CaroTheme/CaroTextField)으로 각색.
> Caro 컨벤션 단일 출처: [`../_conventions/SKILL.md`](../_conventions/SKILL.md)

## Caro 적용 노트

- `core:designsystem` 재사용 컴포넌트는 `@Composable () -> Unit` 슬롯으로 content를 caller에 위임한다.
  실제 예 `CaroTextField`: `header`, `footer`, `trailingIcon`을 `@Composable (() -> Unit)? = null` 슬롯으로 받음.
- 기본 helper는 `XxxDefaults`에, 색/타이포는 `CaroTheme.color/typography` 토큰으로.
- 단일 사용 컴포넌트나 "모든 caller가 똑같이 보여야 하는" primitive(예: 고정 스타일 `Heading`)는 슬롯화하지 않는다.

## Core principle

재사용 컴포넌트의 일은 **무엇을 배치하는지 나열하는 게 아니라 배치하는 것**이다.
`title: String, subtitle: String?, leadingIcon: ImageVector?, trailingText: String?, showSwitch: Boolean...`를
쓰는 순간 컴포넌트는 레이아웃 기술을 멈추고 call site를 나열하기 시작한다 — 다음 call site는 또 없는 파라미터를 원한다.

수정은 `@Composable` 람다 파라미터로 **content를 caller에 위임**하는 것. 컴포넌트는 구조(어디에 leading/headline/
trailing이 가는지)를 기여하고, caller는 그 슬롯에 들어갈 모든 것을 기여한다. Material3 `ListItem`이 정석.

## 1. primitive content를 @Composable 슬롯으로

```kotlin
// ❌ BAD — primitive 파라미터; trailing만 슬롯, 나머지는 고정
@Composable
fun SettingsRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
) { … }

// ✅ GOOD — 모든 시각 영역이 슬롯 (CaroTextField의 header/footer/trailingIcon과 동일 정신)
@Composable
fun SettingsRow(
    headlineContent: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) { … }
```

call site는 짧게 유지되고(`{ Text("Account") }`), Badge/아바타 같은 케이스도 새 파라미터 없이 해결된다.

### 슬롯 네이밍

- 자유형 슬롯은 `xxxContent`(`headlineContent`, `trailingContent`) — Material3 일치.
- 의미가 제약되고 컴포넌트명이 모호성을 없애면 단수 명사(`title`, `actions`, `header`, `footer` — Caro `CaroTextField` 스타일).
- 한 컴포넌트에 `content`와 다른 `xxxContent`를 섞지 말 것.

## 2. 슬롯이 레이아웃으로 emit하면 scope receiver

슬롯 content가 `Row`/`Column`/`Box`에 들어가고 그 레이아웃 기능(`Modifier.weight`, 정렬)이 caller에 필요하면
receiver 람다로:

```kotlin
// ✅ caller가 RowScope를 받아 .weight() 사용 가능
@Composable
fun MyTopBar(
    title: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
)
```

receiver는 실제 부모 레이아웃에 맞춘다(`BoxScope`/`ColumnScope`). 반사적으로 모든 슬롯에 붙이지 말 것.

## 3. 선택적 슬롯 — nullable + null 기본값

```kotlin
// ❌ BAD — 빈 람다 기본값; 레이아웃이 빈 슬롯 공간을 여전히 할당
leadingContent: @Composable () -> Unit = {}

// ✅ GOOD — null이면 "슬롯 없음"; 컴포넌트가 공간/패딩을 생략 가능 (CaroTextField가 이 방식)
leadingContent: (@Composable () -> Unit)? = null
```

`CaroTextField`도 `header`/`footer`/`trailingIcon`을 nullable로 받아 `?.invoke()` / `!= null` 분기로 공간을 생략한다.

## 4. 기본값은 XxxDefaults 에

"보통 chevron", "기본 배경색"을 문서화하게 되면 컴포넌트 옆 `XxxDefaults` object에 co-locate:

```kotlin
object SettingsRowDefaults {
    @Composable
    fun Chevron() = Icon(
        painter = painterResource(Res.drawable.ic_chevron_right_24),
        contentDescription = null,
        tint = CaroTheme.color.icon.tertiary,
    )
}
```

Material3의 `ButtonDefaults`, Caro의 토큰 기반 기본값과 동일 정신. composable 기본값을 새 파라미터로 inline하지 말 것.

## Quick reference

| 증상 | 진단 | 수정 |
|---|---|---|
| 재사용 컴포넌트에 `title: String, leadingIcon: ImageVector?` | primitive content (§1) | `xxxContent: (@Composable () -> Unit)?` 슬롯으로 |
| trailing shape 고르는 boolean 플래그(`showChevron`) | shape 나열 (§1) | 하나의 `trailingContent` 슬롯 |
| `Row` body 안 `actions: @Composable () -> Unit = {}` | scope receiver 누락 (§2) | `@Composable RowScope.() -> Unit = {}` |
| 선택 영역에 `slot: @Composable () -> Unit = {}` | 빈 람다 기본값 (§3) | `(@Composable () -> Unit)? = null` + 분기 |
| 파라미터에 `color: Color = CaroTheme.color...` inline | 기본값 inline (§4) | `XxxDefaults`로 이동 |

## 적용 안 되는 경우

- **단일 사용 컴포넌트.** 슬롯 indirection이 오히려 읽기 어려움 — primitive + inline content 괜찮음. (두 번째 call site 생기면 슬롯화.)
- **모든 caller가 동일해야 하는 디자인시스템 primitive**(`Heading2(text: String)`) — 일관성이 목적이면 primitive 유지.
- **의도적으로 컴포넌트가 소유하는 의미 파라미터**(타이포·접근성 문구·제품 일관성).
- **`Switch(checked, onCheckedChange)`** 같은 boolean+콜백 — "content"가 아니다.

## 리뷰 시 위험 신호

| 생각 | 현실 |
|---|---|
| "title은 *항상* String이라 슬롯은 과설계" | "오늘은 항상"이 함정. `Text + Badge`가 내일 필요. 슬롯은 call site당 `{ Text(…) }` 몇 글자, 나중 추가는 모든 call site를 수정. |
| "람다가 String보다 무겁다" | 일반 UI 규모에선 측정 불가 — 프레임워크 컴포넌트도 전부 슬롯. |
| "trailing만 슬롯하고 leading은 항상 아이콘" | 부분-슬롯 함정. 아바타/이모지가 필요한 순간 깨짐. leading도 슬롯. |

## 관련

- [`../compose-state-holder-ui-split/SKILL.md`](../compose-state-holder-ui-split/SKILL.md) — 디자인시스템 primitive는 상태홀더가 아니라 슬롯/modifier 노출.
- [`../_conventions/SKILL.md`](../_conventions/SKILL.md) — CaroTheme 토큰, designsystem 규칙.
