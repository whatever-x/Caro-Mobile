package com.whatever.caro.feature.importer.component

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.PauseReason
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 시스템 뒤로 가기를 흉내 내지 않고 호스트가 넘긴 닫기 콜백을 직접 부른다. 창 속성은 닫기 정책이 정한 값만 확인한다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class ImportDialogsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<ImportDeckIntent>()

    private fun show(dialog: ImportDialog?): RecordingDialogFrame = composeRule.showImportDialog(dialog, onIntent = { intents += it })

    private fun dismiss(recording: RecordingDialogFrame) {
        composeRule.runOnIdle { recording.onDismissRequest!!.invoke() }
    }

    @Test
    fun `대화상자가 없으면 아무것도 그리지 않는다`() {
        val recording = show(dialog = null)

        recording.properties shouldBe null
    }

    @Test
    fun `기본 틀은 대화상자 창으로 그린다`() {
        composeRule.setContent {
            CaroTheme {
                ImportDialogs(dialog = ImportDialog.DiscardConfirm, onIntent = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNode(isDialog()).assertExists()
    }

    @Test
    fun `지원하지 않는 파일 팝업의 닫기 요청은 확인과 같은 intent를 보낸다`() {
        val recording = show(ImportDialog.ParseError(ImportParseErrorKind.UNSUPPORTED_FILE, extension = "xls", isPaste = false))

        dismiss(recording)

        intents shouldBe listOf(ImportDeckIntent.DismissDialog)
    }

    @Test
    fun `가져온 내용 버리기 팝업의 닫기 요청은 취소와 같은 intent를 보낸다`() {
        val recording = show(ImportDialog.DiscardConfirm)

        dismiss(recording)

        intents shouldBe listOf(ImportDeckIntent.DismissDialog)
    }

    @Test
    fun `가져오기 멈춤 확인 팝업의 닫기 요청은 계속 가져오기와 같은 intent를 보낸다`() {
        val recording = show(ImportDialog.StopConfirm)

        dismiss(recording)

        intents shouldBe listOf(ImportDeckIntent.DismissDialog)
    }

    @Test
    fun `저장이 잠시 멈춘 팝업의 닫기 요청은 아무 intent도 보내지 않는다`() {
        val recording = show(ImportDialog.Paused(saved = 1_000, reason = PauseReason.NETWORK))

        dismiss(recording)

        intents shouldBe emptyList()
    }

    @Test
    fun `일부 카드를 저장하지 못한 팝업의 닫기 요청은 아무 intent도 보내지 않는다`() {
        val recording = show(ImportDialog.PartialFailure(saved = 1_000))

        dismiss(recording)

        intents shouldBe emptyList()
    }

    @Test
    fun `카드를 더 저장할 수 없는 팝업의 닫기 요청은 아무 intent도 보내지 않는다`() {
        val recording = show(ImportDialog.KeyConflict(saved = 1_000))

        dismiss(recording)

        intents shouldBe emptyList()
    }

    @Test
    fun `덱을 찾을 수 없는 팝업의 닫기 요청은 아무 intent도 보내지 않는다`() {
        val recording = show(ImportDialog.DeckUnavailable)

        dismiss(recording)

        intents shouldBe emptyList()
    }

    @Test
    fun `닫을 수 있는 팝업은 뒤로 가기와 바깥 탭을 닫기 요청으로 받는다`() {
        val recording = show(ImportDialog.DiscardConfirm)

        recording.properties!!.dismissOnBackPress shouldBe true
        recording.properties!!.dismissOnClickOutside shouldBe true
    }

    @Test
    fun `닫을 수 없는 팝업은 뒤로 가기와 바깥 탭을 닫기 요청으로 받지 않는다`() {
        val recording = show(ImportDialog.Paused(saved = 0, reason = PauseReason.LATER))

        recording.properties!!.dismissOnBackPress shouldBe false
        recording.properties!!.dismissOnClickOutside shouldBe false
    }

    @Test
    fun `팝업은 플랫폼 기본 너비를 쓰지 않는다`() {
        val recording = show(ImportDialog.DiscardConfirm)

        recording.properties!!.usePlatformDefaultWidth shouldBe false
    }

    @Test
    fun `제목은 heading으로 노출된다`() {
        show(ImportDialog.DiscardConfirm)

        composeRule.titleNode().assertTextEquals("가져온 내용을 버릴까요?")
    }

    @Test
    fun `정보 팝업의 제목도 heading으로 노출된다`() {
        show(ImportDialog.EmptyResult(isPaste = false))

        composeRule.titleNode().assertTextEquals("가져올 카드가 없어요")
    }

    @Test
    fun `팝업이 열리면 제목에 포커스를 둔다`() {
        show(ImportDialog.DiscardConfirm)

        composeRule.titleNode().assertIsFocused()
    }

    @Test
    fun `저장한 카드가 0장이면 멈춤 팝업 본문은 저장한 카드가 없다는 문장으로 시작한다`() {
        show(ImportDialog.Paused(saved = 0, reason = PauseReason.NETWORK))

        composeRule.textContaining(NO_SAVED_CARDS) shouldStartWith NO_SAVED_CARDS
    }

    @Test
    fun `저장한 카드가 0장이면 일부 실패 팝업 본문은 저장한 카드가 없다는 문장으로 시작한다`() {
        show(ImportDialog.PartialFailure(saved = 0))

        composeRule.textContaining(NO_SAVED_CARDS) shouldStartWith NO_SAVED_CARDS
    }

    @Test
    fun `저장한 카드가 0장이면 더 저장할 수 없는 팝업 본문은 저장한 카드가 없다는 문장으로 시작한다`() {
        show(ImportDialog.KeyConflict(saved = 0))

        composeRule.textContaining(NO_SAVED_CARDS) shouldStartWith NO_SAVED_CARDS
    }

    @Test
    fun `멈춤 팝업 본문은 두 문장을 줄바꿈 하나로 잇는다`() {
        show(ImportDialog.Paused(saved = 1_000, reason = PauseReason.NETWORK))

        val body = composeRule.textContaining("1,000장까지 저장했어요.")

        body shouldBe "1,000장까지 저장했어요.\n네트워크를 확인하고 다시 시도해 주세요."
        body.count { char -> char == '\n' } shouldBe 1
    }

    @Test
    fun `일부 실패 팝업 본문은 두 문장을 줄바꿈 하나로 잇는다`() {
        show(ImportDialog.PartialFailure(saved = 1_000))

        val body = composeRule.textContaining("1,000장까지 저장했어요.")

        body shouldBe "1,000장까지 저장했어요.\n다시 시도하거나 저장한 카드로 덱을 볼 수 있어요."
        body.count { char -> char == '\n' } shouldBe 1
    }

    @Test
    fun `더 저장할 수 없는 팝업 본문은 두 문장을 줄바꿈 하나로 잇는다`() {
        show(ImportDialog.KeyConflict(saved = 1_000))

        val body = composeRule.textContaining("1,000장까지 저장했어요.")

        body shouldBe "1,000장까지 저장했어요.\n저장한 카드로 덱을 볼 수 있어요."
        body.count { char -> char == '\n' } shouldBe 1
    }

    private companion object {
        const val NO_SAVED_CARDS = "아직 저장한 카드가 없어요."
    }
}
