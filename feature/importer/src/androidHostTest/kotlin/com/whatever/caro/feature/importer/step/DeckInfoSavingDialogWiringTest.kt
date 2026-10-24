package com.whatever.caro.feature.importer.step

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.PauseReason
import com.whatever.caro.feature.importer.mvi.SavingProgress
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 단계 화면이 비어 있어도 대화상자는 뜬다. 단계 화면을 채운 뒤에도 같은 대화상자가 위에 남고 버튼이 intent로 이어지는지 고정한다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class DeckInfoSavingDialogWiringTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<ImportDeckIntent>()

    private fun show(state: ImportDeckState) {
        composeRule.setContent {
            CaroTheme {
                ImportDeckScreen(state = state, onIntent = { intents += it })
            }
        }
        composeRule.waitForIdle()
    }

    private fun deckInfoWith(dialog: ImportDialog) =
        ImportDeckState(
            step = ImportStep.DeckInfo,
            preview = PreviewStepTables.preview(),
            deckName = "JLPT N5 단어",
            deckDescription = "JLPT N5 단어.apkg에서 가져왔어요",
            dialog = dialog,
        )

    private fun savingWith(dialog: ImportDialog) =
        ImportDeckState(
            step = ImportStep.Saving,
            saving = SavingProgress(deckName = "JLPT N5 단어", totalCards = 1_512, savedCards = 1_000),
            dialog = dialog,
        )

    @Test
    fun `덱 정보 단계에서 덱 생성 실패 대화상자가 뜬다`() {
        show(deckInfoWith(ImportDialog.DeckCreateFailed))

        composeRule.onNodeWithText("덱을 만들지 못했어요").assertIsDisplayed()
    }

    @Test
    fun `덱 생성 실패 대화상자의 다시 시도는 덱 생성 재시도 intent를 보낸다`() {
        show(deckInfoWith(ImportDialog.DeckCreateFailed))

        composeRule.onNodeWithText("다시 시도").performClick()

        intents shouldBe listOf(ImportDeckIntent.RetryCreateDeck)
    }

    @Test
    fun `저장 중 단계에서 멈춤 확인 대화상자가 뜬다`() {
        show(savingWith(ImportDialog.StopConfirm))

        composeRule.onNodeWithText("가져오기를 멈출까요?").assertIsDisplayed()
    }

    @Test
    fun `멈춤 확인 대화상자의 멈추기는 멈춤 확정 intent를 보낸다`() {
        show(savingWith(ImportDialog.StopConfirm))

        composeRule.onNodeWithText("멈추기").performClick()

        intents shouldBe listOf(ImportDeckIntent.ConfirmStop)
    }

    @Test
    fun `저장 중 단계에서 저장이 멈춘 대화상자가 뜬다`() {
        show(savingWith(ImportDialog.Paused(saved = 1_000, reason = PauseReason.NETWORK)))

        composeRule.onNodeWithText("저장이 잠시 멈췄어요").assertIsDisplayed()
    }

    @Test
    fun `저장 중 단계에서 일부 카드를 저장하지 못한 대화상자가 뜬다`() {
        show(savingWith(ImportDialog.PartialFailure(saved = 1_000)))

        composeRule.onNodeWithText("일부 카드를 저장하지 못했어요").assertIsDisplayed()
    }

    @Test
    fun `저장 중 단계에서 카드를 더 저장할 수 없는 대화상자가 뜬다`() {
        show(savingWith(ImportDialog.KeyConflict(saved = 1_000)))

        composeRule.onNodeWithText("카드를 더 저장할 수 없어요").assertIsDisplayed()
    }

    @Test
    fun `저장 중 단계에서 덱을 찾을 수 없는 대화상자가 뜬다`() {
        show(savingWith(ImportDialog.DeckUnavailable))

        composeRule.onNodeWithText("덱을 찾을 수 없어요").assertIsDisplayed()
    }

    @Test
    fun `저장 중 단계의 대화상자는 하나만 뜬다`() {
        show(savingWith(ImportDialog.StopConfirm))

        composeRule.onAllNodes(isDialog()).assertCountEquals(1)
    }
}
