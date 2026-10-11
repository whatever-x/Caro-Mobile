package com.whatever.caro.feature.importer.step

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
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

// 덱 정보와 저장 중 단계는 Screen이 상태의 값을 골라 넘기고 intent로 되돌려야 하므로 Screen을 통해 확인한다.
// 가져오기 버튼의 활성 여부는 상태의 isDeckInfoConfirmEnabled가 정하므로 입력이 비었을 때의 꺼짐도 여기서 본다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class DeckInfoSavingWiringTest {
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

    private fun deckInfoState(
        name: String = NAME,
        description: String = DESCRIPTION,
    ) = ImportDeckState(
        step = ImportStep.DeckInfo,
        preview = PreviewStepTables.preview(),
        deckName = name,
        deckDescription = description,
    )

    private fun savingState(
        savedCards: Int = 1_000,
        status: SavingStatus = SavingStatus.Saving,
    ) = ImportDeckState(
        step = ImportStep.Saving,
        saving = SavingProgress(deckName = NAME, totalCards = 1_512, savedCards = savedCards, status = status),
    )

    @Test
    fun `덱 정보 단계는 상태의 이름을 이름 입력칸에 그린다`() {
        show(deckInfoState())

        composeRule.onNode(hasSetTextAction() and hasText(NAME)).assertExists()
    }

    @Test
    fun `덱 정보 단계는 상태의 설명을 설명 입력칸에 그린다`() {
        show(deckInfoState())

        composeRule.onNode(hasSetTextAction() and hasText(DESCRIPTION)).assertExists()
    }

    @Test
    fun `덱 정보 단계는 미리보기의 카드 수를 안내 줄에 그린다`() {
        show(deckInfoState())

        composeRule.onNodeWithText("카드 8장으로 새 덱을 만들어요.").assertExists()
    }

    @Test
    fun `덱 정보 단계에서 이름을 바꾸면 이름 변경 intent를 보낸다`() {
        show(deckInfoState())

        composeRule.onNode(hasSetTextAction() and hasText(NAME)).performTextReplacement("TOEIC 3000")

        intents shouldBe listOf(ImportDeckIntent.UpdateDeckName("TOEIC 3000"))
    }

    @Test
    fun `덱 정보 단계에서 설명을 바꾸면 설명 변경 intent를 보낸다`() {
        show(deckInfoState())

        composeRule.onNode(hasSetTextAction() and hasText(DESCRIPTION)).performTextReplacement("토익 단어")

        intents shouldBe listOf(ImportDeckIntent.UpdateDeckDescription("토익 단어"))
    }

    @Test
    fun `덱 정보 단계에서 가져오기를 누르면 덱 만들기 intent를 보낸다`() {
        show(deckInfoState())

        composeRule.onNodeWithText("가져오기").performClick()

        intents shouldBe listOf(ImportDeckIntent.ClickCreateDeck)
    }

    @Test
    fun `이름과 설명이 있으면 가져오기 버튼이 켜진다`() {
        show(deckInfoState())

        composeRule.onNodeWithText("가져오기").assertIsEnabled()
    }

    @Test
    fun `이름이 비어 있으면 가져오기 버튼이 꺼진다`() {
        show(deckInfoState(name = ""))

        composeRule.onNodeWithText("가져오기").assertIsNotEnabled()
    }

    @Test
    fun `설명이 비어 있으면 가져오기 버튼이 꺼진다`() {
        show(deckInfoState(description = ""))

        composeRule.onNodeWithText("가져오기").assertIsNotEnabled()
    }

    @Test
    fun `이름이 공백뿐이면 가져오기 버튼이 꺼진다`() {
        show(deckInfoState(name = "   "))

        composeRule.onNodeWithText("가져오기").assertIsNotEnabled()
    }

    @Test
    fun `저장 중 단계는 상태의 덱 이름을 그린다`() {
        show(savingState())

        composeRule.onNodeWithText(NAME).assertExists()
    }

    @Test
    fun `저장 중 단계는 상태의 저장 장수와 전체 장수를 그린다`() {
        show(savingState(savedCards = 1_000))

        composeRule.onNodeWithText("1,000 / 1,512장").assertExists()
    }

    @Test
    fun `저장 중 단계에서 가져오기 멈추기를 누르면 멈추기 intent를 보낸다`() {
        show(savingState())

        composeRule.onNodeWithText("가져오기 멈추기").performClick()

        intents shouldBe listOf(ImportDeckIntent.ClickStop)
    }

    @Test
    fun `저장 중 단계는 연결을 다시 시도하는 상태를 다시 시도 문구로 그린다`() {
        show(savingState(status = SavingStatus.Retrying))

        composeRule.onNodeWithText("연결이 끊겨 다시 시도하고 있어요").assertExists()
    }

    @Test
    fun `저장 중 단계는 서버가 처리 중인 상태를 마무리 문구로 그린다`() {
        show(savingState(status = SavingStatus.WaitingForServer))

        composeRule.onNodeWithText("저장을 마무리하고 있어요").assertExists()
    }

    @Test
    fun `저장 중 단계는 멈추는 중 상태를 저장한 장수와 함께 그린다`() {
        show(savingState(savedCards = 1_000, status = SavingStatus.Stopping))

        composeRule.onNodeWithText("멈추는 중 · 1,000장 저장됨").assertExists()
    }

    @Test
    fun `저장 정보가 아직 없으면 저장 중 화면을 그리지 않는다`() {
        show(ImportDeckState(step = ImportStep.Saving, saving = null))

        composeRule.onNodeWithText("가져오기 멈추기").assertDoesNotExist()
    }

    private companion object {
        const val NAME = "JLPT N5 단어"
        const val DESCRIPTION = "JLPT N5 단어.apkg에서 가져왔어요"
    }
}
