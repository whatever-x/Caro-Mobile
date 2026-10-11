package com.whatever.caro.feature.importer.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportStep
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 로딩 카드의 무한 반복 Lottie 때문에 자동 시계를 켜 두면 waitForIdle이 끝나지 않으므로 시계를 직접 움직인다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class ImportLoadingOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var loading by mutableStateOf<ImportLoading?>(null)
    private var screenState by mutableStateOf(ImportDeckState())
    private var clickCount = 0
    private val intents = mutableListOf<ImportDeckIntent>()

    private val isPoliteLiveRegion = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    private fun showOverlay(initial: ImportLoading?) {
        loading = initial
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CaroTheme {
                ImportLoadingOverlay(loading = loading) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clickable { clickCount += 1 },
                    ) {
                        BasicText(text = CONTENT_TEXT)
                    }
                }
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun showScreen(initial: ImportDeckState) {
        screenState = initial
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CaroTheme {
                ImportDeckScreen(state = screenState, onIntent = { intents += it })
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
    }

    // 자동 시계를 끄면 상태 변경 알림이 대기 중이라, 프레임을 움직이기 전에 한 번 비워야 다시 그려진다.
    private fun applyStateChange() {
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeByFrame()
    }

    @Test
    fun `200ms 전에는 로딩 카드 글자가 없다`() {
        showOverlay(ImportLoading.Parsing(fileName = "deck.apkg"))

        composeRule.mainClock.advanceTimeBy(100)

        composeRule.onNodeWithText("파일을 읽고 있어요").assertDoesNotExist()
    }

    @Test
    fun `200ms 뒤에는 로딩 카드가 보이고 live region으로 알린다`() {
        showOverlay(ImportLoading.Parsing(fileName = "deck.apkg"))

        composeRule.mainClock.advanceTimeBy(200)

        composeRule.onNodeWithText("파일을 읽고 있어요").assertIsDisplayed()
        composeRule.onNode(isPoliteLiveRegion).assertTextContains("파일을 읽고 있어요")
    }

    @Test
    fun `파일을 읽는 중 카드는 파일 이름을 함께 보여준다`() {
        showOverlay(ImportLoading.Parsing(fileName = "deck.apkg"))

        composeRule.mainClock.advanceTimeBy(200)

        composeRule.onNode(isPoliteLiveRegion).assertTextContains("deck.apkg")
    }

    @Test
    fun `붙여 넣은 내용을 읽는 중 카드는 내용을 읽는다는 문구를 보여준다`() {
        showOverlay(ImportLoading.Parsing(fileName = null))

        composeRule.mainClock.advanceTimeBy(200)

        composeRule.onNodeWithText("내용을 읽고 있어요").assertIsDisplayed()
    }

    @Test
    fun `고른 항목을 다시 읽는 중 카드는 파일을 읽는다는 문구와 파일 이름을 보여준다`() {
        showOverlay(ImportLoading.Reparsing(fileName = "deck.apkg"))

        composeRule.mainClock.advanceTimeBy(200)

        composeRule.onNode(isPoliteLiveRegion).assertTextContains("파일을 읽고 있어요")
        composeRule.onNode(isPoliteLiveRegion).assertTextContains("deck.apkg")
    }

    @Test
    fun `미리보기를 만드는 중 카드는 미리보기 문구를 보여준다`() {
        showOverlay(ImportLoading.BuildingPreview)

        composeRule.mainClock.advanceTimeBy(200)

        composeRule.onNodeWithText("미리보기를 만들고 있어요").assertIsDisplayed()
    }

    @Test
    fun `로딩이 아니면 내용을 보여주고 로딩 카드는 없다`() {
        showOverlay(null)

        composeRule.mainClock.advanceTimeBy(300)

        composeRule.onNodeWithText(CONTENT_TEXT).assertIsDisplayed()
        composeRule.onNode(isPoliteLiveRegion).assertDoesNotExist()
    }

    @Test
    fun `로딩 중에는 200ms 전에도 밑에 깔린 내용의 semantics 노드가 없다`() {
        showOverlay(ImportLoading.Parsing(fileName = "deck.apkg"))

        composeRule.onNodeWithText(CONTENT_TEXT).assertDoesNotExist()
    }

    @Test
    fun `로딩 중에는 밑에 깔린 내용으로 가는 터치를 바로 막는다`() {
        showOverlay(ImportLoading.Parsing(fileName = "deck.apkg"))

        composeRule.onRoot().performTouchInput { click() }

        clickCount shouldBe 0
    }

    @Test
    fun `로딩이 아니면 터치가 내용에 전달된다`() {
        showOverlay(null)

        composeRule.onRoot().performTouchInput { click() }

        clickCount shouldBe 1
    }

    @Test
    fun `로딩이 끝나면 내용이 접근성 트리로 돌아온다`() {
        showOverlay(ImportLoading.Parsing(fileName = "deck.apkg"))

        composeRule.runOnIdle { loading = null }
        applyStateChange()

        composeRule.onNodeWithText(CONTENT_TEXT).assertExists()
    }

    @Test
    fun `읽는 중에도 상단 바 뒤로 가기 버튼을 누르면 뒤로 가기를 보낸다`() {
        showScreen(ImportDeckState(loading = ImportLoading.Parsing(fileName = "deck.apkg")))
        composeRule.mainClock.advanceTimeBy(250)

        composeRule.onNodeWithContentDescription(BACK_DESCRIPTION).performClick()

        intents shouldBe listOf(ImportDeckIntent.ClickBack)
    }

    @Test
    fun `덱을 만드는 중에는 상단 바 뒤로 가기 버튼이 접근성 트리에서 빠진다`() {
        showScreen(ImportDeckState(step = ImportStep.CreatingDeck))

        composeRule.onNodeWithContentDescription(BACK_DESCRIPTION).assertDoesNotExist()
    }

    @Test
    fun `덱을 만드는 중에는 상단 바 뒤로 가기 버튼 자리를 눌러도 intent를 보내지 않는다`() {
        showScreen(ImportDeckState(loading = ImportLoading.Parsing(fileName = "deck.apkg")))
        val backCenter: Offset =
            composeRule
                .onNodeWithContentDescription(BACK_DESCRIPTION)
                .fetchSemanticsNode()
                .boundsInRoot.center

        screenState = ImportDeckState(step = ImportStep.CreatingDeck)
        applyStateChange()
        composeRule.onRoot().performTouchInput { click(backCenter) }

        intents shouldBe emptyList()
    }

    @Test
    fun `덱을 만드는 중 카드는 200ms 뒤에 덱을 만든다는 문구를 보여준다`() {
        showScreen(ImportDeckState(step = ImportStep.CreatingDeck))

        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithText("덱을 만들고 있어요").assertDoesNotExist()
        composeRule.mainClock.advanceTimeBy(150)

        composeRule.onNodeWithText("덱을 만들고 있어요").assertIsDisplayed()
    }

    private companion object {
        const val CONTENT_TEXT = "로딩 아래 내용"
        const val BACK_DESCRIPTION = "뒤로 가기"
    }
}
