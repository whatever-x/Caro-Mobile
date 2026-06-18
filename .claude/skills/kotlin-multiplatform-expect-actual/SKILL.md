---
name: kotlin-multiplatform-expect-actual
description: >
  KMP에서 플랫폼 서비스·네이티브 SDK·소스셋·Compose Multiplatform UI·권한·파일·설정·센서 등의
  expect/actual 또는 인터페이스 경계를 설계할 때 사용한다. "expect/actual", "플랫폼 분기",
  "iOS/Android 구현 분리", "commonMain에 어떻게", "actual 구현", "플랫폼 인터페이스" 맥락에서 발동.
  Caro는 commonMain 공용 API + androidMain/iosMain actual(`*.android.kt`/`*.ios.kt`) 패턴을 쓴다.
---

# KMP: expect/actual 경계

> 출처: chrisbanes/skills (Apache-2.0) · 벤더링 2026-06-09. 원문 기술 내용을 유지하되 Caro 컨벤션 노트를 추가함.
> Caro 컨벤션 단일 출처: [`../_conventions/SKILL.md`](../_conventions/SKILL.md)

## Caro 적용 노트

- 플랫폼 분기는 `expect`/`actual` + 파일명 규칙 **`Foo.android.kt` / `Foo.ios.kt`** (`commonMain`에 `expect`).
  실제 예: `core/remote/.../network/HttpClientEngineProvider.{kt, android.kt, ios.kt}` (OkHttp / Darwin).
- iOS 라이브러리는 SPM4KMP로 통합한다(최근 마이그레이션됨). 네이티브 SDK는 actual 안에 숨기고 commonMain엔 의미적 API만 노출.
- DI·테스트 페이크·런타임 선택이 필요하면 `expect class`가 아니라 **commonMain 인터페이스 + Koin 플랫폼 바인딩**을 쓴다
  (Caro는 Koin 단축 DSL — [`../_conventions/SKILL.md`](../_conventions/SKILL.md) 참고).

## Core principle

공용(common) API는 의미적(semantic)이고 안정적으로 유지한다. 플랫폼 메커니즘은 작은 `expect`/`actual`
선언이나 인터페이스 뒤에 숨기고, Android/iOS/Desktop 세부사항을 `commonMain` 밖으로 내보낸다.

## 언제 쓰나

공용 코드가 다음을 필요로 할 때:

- 권한, 설정, 공유 시트, 딥링크, 햅틱, 생체인증, 클립보드.
- 파일, 경로, 시계, 로케일, 네트워크 도달성, 센서, 암호화, 미디어, 카메라, 네이티브 SDK.
- 네이티브 뷰/컨트롤러, Compose Multiplatform interop.
- Android/iOS에서 구현은 다르지만 호출 지점은 하나로 공유하고 싶을 때.
- `expect/actual` vs DI vs 인터페이스 vs 분리된 플랫폼 코드 사이의 선택.

## 경계 고르기

| 상황 | 선호 |
|---|---|
| 단순 컴파일타임 플랫폼 특수화 | `expect`/`actual` 함수·값·typealias·leaf composable |
| 주입 의존성·생명주기 소유·런타임 선택·테스트 페이크가 필요 | commonMain 인터페이스 + 플랫폼 바인딩 |
| UI 대부분 공유, 한 leaf만 다름 | `expect` leaf를 호출하는 공용 composable |
| 화면 전체가 플랫폼별로 다름 | 공용 네비게이션 계약 뒤의 분리된 플랫폼 화면 |
| 상수/리소스만 다름 | 의미적 값을 노출하는 공용 API, 플랫폼별 actual 값 |

## 공용 API는 의미적으로

공용 코드는 플랫폼이 *어떻게* 하는지가 아니라 제품이 *무엇*을 필요로 하는지 기술한다:

```kotlin
// GOOD: 의미적 공용 API
expect fun currentRegion(): Region

// BAD: Android 구현이 새어나옴
expect fun currentRegionFromAndroidLocale(context: Context): Region
```

Android actual은 `Locale` API를, iOS actual은 Foundation API를 써도 된다. 호출자는 몰라야 한다.

## actual은 얇게

actual 구현은 의미적 API를 플랫폼 호출로 번역만 한다. Activity, view controller, 생명주기 소유자, DI,
페이크가 필요하면 `expect class` 대신 **플랫폼이 공급하는 인터페이스**를 선호한다:

```kotlin
// commonMain
interface ShareSheet {
    suspend fun shareText(text: String)
}
```

```kotlin
// androidMain (ShareSheet.android.kt 등)
class AndroidShareSheet(
    private val activity: Activity,
) : ShareSheet {
    override suspend fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
        activity.startActivity(Intent.createChooser(intent, null))
    }
}
```

actual에 비즈니스 규칙이 쌓이기 시작하면, 그 규칙을 공용 코드로 옮기고 actual에는 플랫폼 번역만 남긴다.

## 테스트/DI가 중요하면 인터페이스

단순 컴파일타임 플랫폼 API엔 `expect/actual`. 공용 코드가 페이크·복수 구현·런타임 선택·생명주기 소유를
필요로 하면 인터페이스:

```kotlin
interface Clipboard {
    suspend fun setText(text: String)
}
```

플랫폼 모듈이 `Clipboard`를 Android/iOS 구현에 바인딩한다(Caro에선 Koin). 공용 테스트는 페이크를 쓴다.

## Compose 관련 가이드

- 플랫폼별 Composable은 leaf 노드에 둔다.
- UI를 그리는 모든 expect Composable에 `Modifier`를 통과시킨다.
- `commonMain` 시그니처에 플랫폼 타입 금지(`Context`, `Activity`, 리소스 ID, `Uri`, `Bundle`,
  `UIViewController`, `NSBundle`, 플랫폼 권한 enum 등).
- 네이티브 뷰 생명주기가 중요하면 actual 안에 숨기고 알맞은 interop 컨테이너(`AndroidView`, `UIKitView`) 사용.
- Composable 본문에서 플랫폼 작업을 직접 실행하지 말 것. actual Composable에서도 `remember`,
  `LaunchedEffect`, `DisposableEffect`, 안정적 key를 공용 Compose와 똑같이 쓴다.
- 미리보기/테스트는 페이크 플랫폼 서비스를 주입한 공용 plain composable로.

## 흔한 실수

| 실수 | 수정 |
|---|---|
| `commonMain` API가 Android/iOS 타입 노출 | 의미적 공용 타입으로 교체 |
| `expect` 함수가 한 플랫폼 전용 파라미터를 가짐 | 그 세부사항을 actual로 이동 |
| 비즈니스 분기가 각 actual에 중복 | 비즈니스 규칙을 공용 코드로 이동 |
| 거대한 단일 `Platform` expect object | 기능별로 분리: `Clipboard`, `ShareSheet`, `Haptics` |
| 플랫폼 UI가 트리 상단에 노출 | 플랫폼별 Composable을 leaf로 밀기 |
| 공용 테스트용 페이크 경계 없음 | 직접 `expect` 호출 대신 인터페이스 |

## 리뷰 시 위험 신호

- 공용 코드가 플랫폼 패키지를 import 한다.
- actual 구현이 제품 상태·네비게이션 결정·도메인 규칙을 안다.
- 플랫폼 API 이름이 공용 함수 이름에 등장한다.
- 세 번째 플랫폼을 추가하려면 공용 호출자를 바꿔야 한다.
- 공용 비즈니스 동작 검증에 Android/iOS 런타임이 필요하다.

## 관련

- [`../compose-state-holder-ui-split/SKILL.md`](../compose-state-holder-ui-split/SKILL.md) — 공용 plain UI vs 상태홀더 배선.
- [`../compose-side-effects/SKILL.md`](../compose-side-effects/SKILL.md) — actual composable의 effect key/cleanup.
- [`../compose-slot-api-pattern/SKILL.md`](../compose-slot-api-pattern/SKILL.md) — 재사용 가능한 공용 Compose API(슬롯).
- [`../_conventions/SKILL.md`](../_conventions/SKILL.md) — Caro 모듈/파일명/DI 규칙.
