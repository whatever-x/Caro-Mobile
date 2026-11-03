# Home 스크린샷 작성 예제

신규 Feature의 시나리오·플랫폼 진입점을 작성하거나 기존 화면 목록을 변경할 때 읽는다.
명령은 저장소 루트에서 사용자에게 허용된 경우에만 실행한다. 빌드·테스트·캡처 실행 금지는 예제 명령보다 우선한다.

## 코드와 의존성

Feature는 기존 KMP·CMP·Feature 설정에 `caro.kmp.test`와 `caro.screenshot`을 적용한다.
플러그인은 `commonTest`에 공통 모듈 의존성을 연결한다.
Feature의 main에 테스트 모듈을 추가하지 않는다.
프로필·캡처·문자열·Geometry 보조는 복사하지 않고 `com.whatever.caro.core.screenshot.testing`에서 import한다.

공통 구현 원본:

- [프로필·Variant](../../../../core/screenshot-testing/src/commonMain/kotlin/com/whatever/caro/core/screenshot/testing/ScreenshotProfile.kt)
- [시나리오·대상·언어 확장·파일명](../../../../core/screenshot-testing/src/commonMain/kotlin/com/whatever/caro/core/screenshot/testing/ScreenshotScenario.kt)
- [문자열 준비](../../../../core/screenshot-testing/src/commonMain/kotlin/com/whatever/caro/core/screenshot/testing/ScreenshotStrings.kt)
- [Canvas·Geometry](../../../../core/screenshot-testing/src/commonMain/kotlin/com/whatever/caro/core/screenshot/testing/ScreenshotCanvas.kt)
- [Android Rule·Locale·Clock·Dialog Window](../../../../core/screenshot-testing/src/androidMain/kotlin/com/whatever/caro/core/screenshot/testing/ScreenshotAndroidCapture.kt)
- [iOS 실행·AppleLanguages 복원](../../../../core/screenshot-testing/src/iosMain/kotlin/com/whatever/caro/core/screenshot/testing/ScreenshotIosCapture.kt)

## Home 상태와 시나리오

현재 Home은 `HomeScreen(state: HomeState, onIntent: (HomeIntent) -> Unit)`을 사용한다.
콘텐츠 fixture에서는 기본값이 true인 `isLoading`을 false로 지정한다. State·Fixture·렌더러·동작 단언은 Feature에 둔다.
다른 화면의 파라미터를 Home과 같다고 가정하지 않는다.

[HomeScreenshotScenario.kt](../../../../feature/home/src/commonTest/kotlin/com/whatever/caro/feature/home/HomeScreenshotScenario.kt):

```kotlin
package com.whatever.caro.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_title_create
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.screenshot.testing.ScreenshotScenario
import com.whatever.caro.core.screenshot.testing.ScreenshotString
import com.whatever.caro.feature.home.mvi.HomeState
import com.whatever.caro.feature.home.mvi.HomeStreakState
import kotlinx.collections.immutable.persistentListOf

internal object HomeScreenshotScenario {
    const val ID = "home.home.with_deck"

    fun scenarios() =
        listOf(
            ScreenshotScenario(
                ID,
                render = { Render() },
                expectedStrings = listOf(ScreenshotString(Res.string.deck_title_create)),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.deck_title_create]).assertIsDisplayed()
                },
            ),
        )

    @Composable
    fun Render() {
        CaroTheme {
            HomeScreen(
                state =
                    HomeState(
                        nickname = "승우",
                        streakState = HomeStreakState.NotStarted,
                        decks = persistentListOf(Deck(1, "Android", "기초 학습", 10, 5, 0, DeckState.NOT_STARTED)),
                        isLoading = false,
                    ),
                onIntent = {
                },
            )
        }
    }
}
```

`expectedStrings`의 대표 리소스는 의미 있는 ko/en 번역 차이가 있어야 한다.
포맷 인자는 `ScreenshotString(Res.string.library_card_count, formatArgs = listOf(1))`처럼 전달한다.
실제 문구와 줄바꿈은 XML에서 관리한다. 단언에 번역 문구를 복사하지 않는다.

## 기대 목록

`scripts/screenshot/scenarios.json`의 해당 모듈 배열에 논리 ID를 등록한다.
다음은 Home 목록의 일부다. 기존 Library ID는 삭제하지 않는다.

```json
{"home": ["home.home.with_deck"]}
```

registry는 기대 목록이며 코드를 생성하지 않는다. 화면별 factory를 두 플랫폼 진입점에 직접 전달한다.
ID는 전체 모듈에서 고유해야 한다. `__` 및 `_actual`·`_compare`·`_before` 접미사는 예약한다.

## Android 진입점

[HomeScreenshotAndroidTest.kt](../../../../feature/home/src/androidHostTest/kotlin/com/whatever/caro/feature/home/HomeScreenshotAndroidTest.kt):

```kotlin
package com.whatever.caro.feature.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.whatever.caro.core.screenshot.testing.ScreenshotCapture
import com.whatever.caro.core.screenshot.testing.captureAndroidScreenshot
import com.whatever.caro.core.screenshot.testing.screenshotAndroidRule
import com.whatever.caro.core.screenshot.testing.screenshotCaptures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
internal class HomeScreenshotAndroidTest(
    private val capture: ScreenshotCapture,
) {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule val captureRule = screenshotAndroidRule(capture, composeRule)

    @Test
    fun `지정한 언어와 프로필로 시나리오 스크린샷을 캡처한다`() = captureAndroidScreenshot(composeRule, capture)

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun captures(): List<Array<Any>> = screenshotCaptures("android", HomeScreenshotScenario::scenarios).map { arrayOf(it) }
    }
}
```

각 case는 새 Compose Rule을 사용한다.
바깥 Locale Rule은 Activity 실행 전에 프로필과 언어를 설정한다. 예외가 발생해도 원래 설정을 복원한다.
문자열 준비와 Canvas·Geometry·Clock·캡처 처리는 공통 실행기에 위임한다.

## iOS 진입점

[HomeScreenshotIosTest.kt](../../../../feature/home/src/iosSimulatorArm64Test/kotlin/com/whatever/caro/feature/home/HomeScreenshotIosTest.kt):

```kotlin
package com.whatever.caro.feature.home

import com.whatever.caro.core.screenshot.testing.captureIosScreenshots
import kotlin.test.Test

class HomeScreenshotIosTest {
    @Test
    fun `모든 시나리오의 한국어와 영어 스크린샷을 캡처한다`() = captureIosScreenshots(HomeScreenshotScenario::scenarios)
}
```

각 ko/en capture는 별도 Skiko 세션을 사용한다. `iosMain` 실행기는 px Size와 Density를 전달한다.
실행기는 앱 도메인에 저장된 AppleLanguages를 finally에서 복원한다.
원래 key가 없으면 key를 제거한다. 이렇게 시스템 fallback을 유지한다.

## Variant와 Dialog 추가

일반 화면은 기본 `SCREEN`·`DEFAULT`를 사용한다. 작은 화면은 시나리오 생성자에 다음 중 필요한 값만 선언한다.

```kotlin
variant = ScreenshotVariant.NARROW // 320×700 논리 크기, fontScale 1
variant = ScreenshotVariant.SMALL_LARGE_FONT // 320×480 논리 크기, fontScale 1.5
```

새 ID에 Variant를 지정하면 세 워크플로의 모든 기대 파일명 분기에 같은 ID와 접미사를 반영한다.
새 Variant 자체를 만들 때는 공통 enum·양 플랫폼 프로필과 CI profile 목록·metadata 검사도 갱신한다.
Library의 `english` ID는 고정 데이터 variant이며 리소스 로케일은 ko/en 모두 실행한다.

Dialog는 시나리오 생성자에 `target = ScreenshotTarget.DIALOG`를 선언한다. 별도 content-sized ID 목록은 없다.
Home 오류·Setting 권한 Dialog처럼 beforeCapture는 표시를, afterCapture는 콜백 횟수를 확인한다.
Android는 유일한 Dialog semantics 노드의 소유 View와 그 Window의 decor를 함께 선택한다.
iOS는 같은 Dialog 노드를 측정·캡처한다. 상세 전제와 미확정 원인은
[Dialog View·Window 계약](../../../../docs/development/screenshot-testing.md#android-dialog의-view와-window)을 따른다.

## 명령과 산출물

다음 명령은 실행이 허용된 작업에서만 사용한다. 공통 모듈을 변경한 경우 Home만으로 회귀 검증을 끝내지 않는다.
등록된 9개 Feature의 Android/iOS·ko/en 전체가 대상이다. CI는 경로 필터 없이 모두 실행한다.

```bash
./gradlew :core:screenshot-testing:testAndroidHostTest
./gradlew :core:screenshot-testing:iosSimulatorArm64Test
./gradlew :feature:home:recordRoborazziAndroidHostTest :feature:home:testAndroidHostTest --rerun
./gradlew :feature:home:recordRoborazziIosSimulatorArm64 :feature:home:iosSimulatorArm64Test --rerun
./gradlew :feature:home:compareRoborazziAndroidHostTest :feature:home:testAndroidHostTest --rerun
./gradlew :feature:home:compareRoborazziIosSimulatorArm64 :feature:home:iosSimulatorArm64Test --rerun
```

출력은 `feature/home/build/outputs/roborazzi/androidHostTest/`와 `iosSimulatorArm64/`에 있다.
파일명은 `home.home.with_deck__ko__pixel10-pro.png`처럼 `<scenario-id>__<language>__<profile-id>.png`다.
JSON은 Feature의 `build/test-results/roborazzi/<target>/results-summary.json`, HTML은 `build/reports/roborazzi/<target>/index.html`이다.
공통 모듈에는 Roborazzi Gradle 플러그인을 적용하지 않으며 Golden 이미지도 수집하지 않는다.
`SCREENSHOT_GEOMETRY`는 Kotlin 단언 로그다. CI는 PNG 내용·치수나 이 로그를 해석하지 않는다.
기존 PNG·record 직후 compare 통과·정적 점검을 변경된 코드의 실행 성공이나 시각적 승인으로 보고하지 않는다.
CI 실행·Golden·이미지 게시 승인은 [스크린샷 CI](../../../../docs/ci/screenshot-test.md)가 원본이다.
