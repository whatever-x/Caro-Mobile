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
import com.whatever.caro.feature.importer.mvi.SavingProgress
import com.whatever.caro.feature.importer.mvi.SavingStatus
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 저장 단계는 Screen이 상태의 저장 진행을 골라 넘기고 버튼을 intent로 되돌려야 하므로 Screen을 통해 확인한다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class ImportDeckScreenSavingTest {
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

    private fun saving(
        savedCards: Int = 1_000,
        status: SavingStatus = SavingStatus.Saving,
        dialog: ImportDialog? = null,
    ) = ImportDeckState(
        step = ImportStep.Saving,
        deckName = NAME,
        saving = SavingProgress(deckName = NAME, totalCards = 1_512, savedCards = savedCards, status = status),
        dialog = dialog,
    )

    @Test
    fun `저장 단계는 상단 제목이 카드 저장이고 덱 이름과 저장 장수를 그린다`() {
        show(saving())

        listOf("카드 저장", NAME, "1,000 / 1,512장").forEach { text -> composeRule.onNodeWithText(text).assertIsDisplayed() }
    }

    @Test
    fun `저장 단계에서 가져오기 멈추기를 누르면 멈추기 intent를 보낸다`() {
        show(saving())

        composeRule.onNodeWithText("가져오기 멈추기").performClick()

        intents shouldBe listOf(ImportDeckIntent.ClickStop)
    }

    @Test
    fun `저장 단계 위에 멈춤 확인이 하나만 뜨고 멈추기는 멈춤 확정 intent를 보낸다`() {
        show(saving(dialog = ImportDialog.StopConfirm))

        composeRule.onAllNodes(isDialog()).assertCountEquals(1)
        composeRule.onNodeWithText("가져오기를 멈출까요?").assertIsDisplayed()
        composeRule.onNodeWithText("멈추기").performClick()

        intents shouldBe listOf(ImportDeckIntent.ConfirmStop)
    }

    private companion object {
        const val NAME = "JLPT N5 단어"
    }
}
