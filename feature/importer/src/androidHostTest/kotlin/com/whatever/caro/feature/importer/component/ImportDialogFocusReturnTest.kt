package com.whatever.caro.feature.importer.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.SavingProgress
import com.whatever.caro.feature.importer.step.PreviewStepTables
import com.whatever.caro.feature.importer.step.SelectionStepTables
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 팝업을 닫으면 팝업을 연 요소로 포커스가 돌아와야 한다. 실제 팝업 창으로 그려 닫기 요청을 처리하는 시점의 포커스까지 확인한다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class ImportDialogFocusReturnTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var screenState by mutableStateOf(ImportDeckState())

    private fun show(state: ImportDeckState) {
        screenState = state
        composeRule.setContent {
            CaroTheme {
                ImportDeckScreen(state = screenState, onIntent = {})
            }
        }
        composeRule.waitForIdle()
    }

    private fun update(next: ImportDeckState) {
        composeRule.runOnIdle { screenState = next }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
    }

    private fun sheetState(dialog: ImportDialog? = null) =
        ImportDeckState(step = ImportStep.SheetSelect, table = SelectionStepTables.xlsx(), dialog = dialog)

    private fun savingState(dialog: ImportDialog? = null) =
        ImportDeckState(step = ImportStep.Saving, saving = SavingProgress(deckName = "일본어", totalCards = 1_512), dialog = dialog)

    private fun deckInfoState(dialog: ImportDialog? = null) =
        ImportDeckState(
            step = ImportStep.DeckInfo,
            preview = PreviewStepTables.preview(),
            deckName = "일본어",
            deckDescription = "단어장",
            dialog = dialog,
        )

    @Test
    fun `가져온 내용 버리기 팝업을 닫으면 뒤로 가기 버튼으로 포커스를 돌려준다`() {
        show(sheetState(dialog = ImportDialog.DiscardConfirm))
        composeRule.onNode(isDialog()).assertExists()

        update(sheetState())

        composeRule.onNodeWithContentDescription("뒤로 가기").assertIsFocused()
    }

    @Test
    fun `가져오기 멈춤 확인 팝업을 닫으면 가져오기 멈추기 버튼으로 포커스를 돌려준다`() {
        show(savingState(dialog = ImportDialog.StopConfirm))
        composeRule.onNode(isDialog()).assertExists()

        update(savingState())

        composeRule.onNode(hasText("가져오기 멈추기")).assertIsFocused()
    }

    @Test
    fun `덱을 만들지 못했어요 팝업을 닫으면 가져오기 버튼으로 포커스를 돌려준다`() {
        show(deckInfoState(dialog = ImportDialog.DeckCreateFailed))
        composeRule.onNode(isDialog()).assertExists()

        update(deckInfoState())

        composeRule.onNodeWithText("가져오기").assertIsFocused()
    }

    @Test
    fun `팝업을 닫으면서 단계가 바뀌면 뒤로 가기 버튼으로 포커스를 옮기지 않는다`() {
        show(sheetState(dialog = ImportDialog.DiscardConfirm))

        update(ImportDeckState())

        composeRule.onNodeWithContentDescription("뒤로 가기").assertIsNotFocused()
    }

    @Test
    fun `읽기 실패 팝업을 닫아도 뒤로 가기 버튼으로 포커스를 옮기지 않는다`() {
        show(ImportDeckState(dialog = ImportDialog.EmptyResult(isPaste = false)))

        update(ImportDeckState())

        composeRule.onNodeWithContentDescription("뒤로 가기").assertIsNotFocused()
    }

    private companion object {
        const val SETTLE_MILLIS = 48L
    }
}
