package com.whatever.caro.feature.importer.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.logic.NoCardsKind
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportStep
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 처음으로 돌아갈지 묻는 대화상자를 닫고 그 화면에 머물면 대화상자를 연 뒤로 가기 버튼으로 포커스가 돌아와야 한다. 실제 대화상자 창으로 그린다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class ImportDialogFocusReturnTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var screenState by mutableStateOf(ImportDeckState())
    private val intents = mutableListOf<ImportDeckIntent>()

    private fun show(state: ImportDeckState) {
        screenState = state
        composeRule.setContent {
            CaroTheme {
                ImportDeckScreen(state = screenState, onIntent = { intent -> intents += intent })
            }
        }
        composeRule.waitForIdle()
    }

    private fun update(next: ImportDeckState) {
        composeRule.runOnIdle { screenState = next }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
    }

    private fun previewState(dialog: ImportDialog? = null) = ImportDeckState(step = ImportStep.Preview, dialog = dialog)

    @Test
    fun `처음으로 돌아갈지 묻는 대화상자를 닫고 머물면 뒤로 가기 버튼으로 포커스를 돌려준다`() {
        show(previewState(dialog = ImportDialog.DiscardConfirm))
        composeRule.onNode(isDialog()).assertExists()

        update(previewState())

        composeRule.onNodeWithContentDescription(BACK_DESCRIPTION).assertIsFocused()
    }

    @Test
    fun `포커스를 돌려받은 뒤로 가기 버튼에서 Enter를 누르면 뒤로 가기를 보낸다`() {
        show(previewState(dialog = ImportDialog.DiscardConfirm))
        update(previewState())
        composeRule.onNodeWithContentDescription(BACK_DESCRIPTION).assertIsFocused()

        composeRule.onNodeWithContentDescription(BACK_DESCRIPTION).performKeyInput { pressKey(Key.Enter) }

        intents shouldBe listOf(ImportDeckIntent.ClickBack)
    }

    @Test
    fun `대화상자를 닫으면서 단계가 바뀌면 뒤로 가기 버튼으로 포커스를 옮기지 않는다`() {
        show(previewState(dialog = ImportDialog.DiscardConfirm))

        update(ImportDeckState())

        composeRule.onNodeWithContentDescription(BACK_DESCRIPTION).assertIsNotFocused()
    }

    @Test
    fun `읽기 실패 대화상자를 닫아도 뒤로 가기 버튼으로 포커스를 옮기지 않는다`() {
        show(ImportDeckState(dialog = ImportDialog.NoCards(NoCardsKind.NoContent)))

        update(ImportDeckState())

        composeRule.onNodeWithContentDescription(BACK_DESCRIPTION).assertIsNotFocused()
    }

    private companion object {
        const val BACK_DESCRIPTION = "뒤로 가기"
        const val SETTLE_MILLIS = 48L
    }
}
