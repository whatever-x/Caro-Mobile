package com.whatever.caro.feature.deck.create

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.deck.create.mvi.CreateDeckIntent
import com.whatever.caro.feature.deck.create.mvi.CreateDeckState
import com.whatever.caro.feature.deck.edit.CreateDeckScreen
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class CreateDeckScreenSemanticsAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<CreateDeckIntent>()

    private fun showScreen(state: CreateDeckState) {
        composeRule.setContent {
            CaroTheme {
                CreateDeckScreen(state = state, onIntent = { intents += it })
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `만들기 버튼은 Role Button으로 노출된다`() {
        showScreen(FILLED_STATE)

        composeRule
            .onNodeWithText(CONFIRM_LABEL)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun `입력이 채워져 있으면 만들기 버튼이 ClickConfirm을 보낸다`() {
        showScreen(FILLED_STATE)

        composeRule.onNodeWithText(CONFIRM_LABEL).performClick()

        intents shouldBe listOf(CreateDeckIntent.ClickConfirm)
    }

    @Test
    fun `입력이 비어 있으면 만들기 버튼을 눌러도 intent를 보내지 않는다`() {
        showScreen(CreateDeckState())

        composeRule.onNodeWithText(CONFIRM_LABEL).performClick()

        intents shouldBe emptyList()
    }

    @Test
    fun `TIP 라벨과 200장 안내를 보여준다`() {
        showScreen(CreateDeckState())

        composeRule.onNodeWithText("TIP").assertIsDisplayed()
        composeRule.onNodeWithText(TIP_LINE).assertIsDisplayed()
    }

    private companion object {
        const val CONFIRM_LABEL = "만들기"
        const val TIP_LINE = "카드는 한 번에 200장까지 추가할 수 있어요."
        val FILLED_STATE = CreateDeckState(name = "Android", description = "기초 학습")
    }
}
