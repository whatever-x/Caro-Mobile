package com.whatever.caro.feature.importer.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.PauseReason
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
class ImportDialogsActionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<ImportDeckIntent>()

    private fun showAndClick(
        dialog: ImportDialog,
        label: String,
    ) {
        composeRule.showImportDialog(dialog, onIntent = { intents += it })
        composeRule.onNodeWithText(label).performClick()
    }

    @Test
    fun `파일이 너무 큰 팝업의 확인은 팝업 닫기를 보낸다`() {
        showAndClick(ImportDialog.FileTooLarge(extension = "apkg"), "확인")

        intents shouldBe listOf(ImportDeckIntent.DismissDialog)
    }

    @Test
    fun `파싱 오류 팝업의 확인은 팝업 닫기를 보낸다`() {
        showAndClick(ImportDialog.ParseError(ImportParseErrorKind.CORRUPT_FILE, extension = "apkg", isPaste = false), "확인")

        intents shouldBe listOf(ImportDeckIntent.DismissDialog)
    }

    @Test
    fun `미리보기 실패 팝업의 확인은 팝업 닫기를 보낸다`() {
        showAndClick(ImportDialog.PreviewFailed, "확인")

        intents shouldBe listOf(ImportDeckIntent.DismissDialog)
    }

    @Test
    fun `버리기 확인 팝업의 버리기는 버리기 확정을 보낸다`() {
        showAndClick(ImportDialog.DiscardConfirm, "버리기")

        intents shouldBe listOf(ImportDeckIntent.ConfirmDiscard)
    }

    @Test
    fun `버리기 확인 팝업의 취소는 팝업 닫기를 보낸다`() {
        showAndClick(ImportDialog.DiscardConfirm, "취소")

        intents shouldBe listOf(ImportDeckIntent.DismissDialog)
    }

    @Test
    fun `덱 생성 실패 팝업의 다시 시도는 덱 생성 재시도를 보낸다`() {
        showAndClick(ImportDialog.DeckCreateFailed, "다시 시도")

        intents shouldBe listOf(ImportDeckIntent.RetryCreateDeck)
    }

    @Test
    fun `덱 생성 실패 팝업의 취소는 팝업 닫기를 보낸다`() {
        showAndClick(ImportDialog.DeckCreateFailed, "취소")

        intents shouldBe listOf(ImportDeckIntent.DismissDialog)
    }

    @Test
    fun `멈춤 확인 팝업의 멈추기는 멈춤 확정을 보낸다`() {
        showAndClick(ImportDialog.StopConfirm, "멈추기")

        intents shouldBe listOf(ImportDeckIntent.ConfirmStop)
    }

    @Test
    fun `멈춤 확인 팝업의 계속 가져오기는 팝업 닫기를 보낸다`() {
        showAndClick(ImportDialog.StopConfirm, "계속 가져오기")

        intents shouldBe listOf(ImportDeckIntent.DismissDialog)
    }

    @Test
    fun `저장이 멈춘 팝업의 다시 시도는 저장 재시도를 보낸다`() {
        showAndClick(ImportDialog.Paused(saved = 1_000, reason = PauseReason.NETWORK), "다시 시도")

        intents shouldBe listOf(ImportDeckIntent.RetrySave)
    }

    @Test
    fun `저장이 멈춘 팝업의 멈추고 덱 보기는 덱 보기를 보낸다`() {
        showAndClick(ImportDialog.Paused(saved = 1_000, reason = PauseReason.NETWORK), "멈추고 덱 보기")

        intents shouldBe listOf(ImportDeckIntent.ClickViewDeck)
    }

    @Test
    fun `일부 실패 팝업의 다시 시도는 저장 재시도를 보낸다`() {
        showAndClick(ImportDialog.PartialFailure(saved = 1_000), "다시 시도")

        intents shouldBe listOf(ImportDeckIntent.RetrySave)
    }

    @Test
    fun `일부 실패 팝업의 멈추고 덱 보기는 덱 보기를 보낸다`() {
        showAndClick(ImportDialog.PartialFailure(saved = 1_000), "멈추고 덱 보기")

        intents shouldBe listOf(ImportDeckIntent.ClickViewDeck)
    }

    @Test
    fun `더 저장할 수 없는 팝업의 덱 보기는 덱 보기를 보낸다`() {
        showAndClick(ImportDialog.KeyConflict(saved = 1_000), "덱 보기")

        intents shouldBe listOf(ImportDeckIntent.ClickViewDeck)
    }

    @Test
    fun `덱을 찾을 수 없는 팝업의 홈으로는 홈 이동을 보낸다`() {
        showAndClick(ImportDialog.DeckUnavailable, "홈으로")

        intents shouldBe listOf(ImportDeckIntent.ClickGoHome)
    }
}
