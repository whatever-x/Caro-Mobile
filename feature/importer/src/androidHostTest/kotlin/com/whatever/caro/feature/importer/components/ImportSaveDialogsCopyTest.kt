package com.whatever.caro.feature.importer.components

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

// 문구는 Figma 표시 문구다. 본문은 저장 줄과 원인 줄을 줄바꿈 하나로 잇고, 버튼은 하려는 동작이 왼쪽이다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class ImportSaveDialogsCopyTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<ImportDeckIntent>()

    private fun show(dialog: ImportDialog) {
        composeRule.showImportDialog(dialog, onIntent = intents::add)
    }

    private fun assertDialog(
        title: String,
        body: String,
        vararg buttons: String,
    ) {
        composeRule.titleNode().assertTextEquals(title)
        composeRule.textContaining(body.substringBefore('\n')) shouldBe body
        composeRule.buttonLabelsLeftToRight() shouldBe buttons.toList()
    }

    @Test
    fun `덱 만들기 실패는 다시 시도가 왼쪽 취소가 오른쪽이고 다시 시도는 덱 다시 만들기를 보낸다`() {
        show(ImportDialog.DeckCreateFailed)

        assertDialog("덱을 만들지 못했어요", CHECK_NETWORK, RETRY, "취소")
        composeRule.onNodeWithText(RETRY).performClick()
        intents shouldBe listOf(ImportDeckIntent.RetryCreateDeck)
    }

    @Test
    fun `멈춤 확인은 두 문장을 줄바꿈으로 잇고 멈추기는 멈춤 확정을 보낸다`() {
        show(ImportDialog.StopConfirm)

        assertDialog(
            "가져오기를 멈출까요?",
            "지금 저장 중인 카드까지만 덱에 남아요.\n남은 카드는 나중에 이어서 가져올 수 없어요.",
            "멈추기",
            "계속 가져오기",
        )
        composeRule.onNodeWithText("멈추기").performClick()
        intents shouldBe listOf(ImportDeckIntent.ConfirmStop)
    }

    @Test
    fun `일부 저장 뒤 일시 정지는 쉼표 뒤에서 줄을 바꾸고 더 저장됐을 수 있다고 안내한다`() {
        show(ImportDialog.Paused(saved = 1_000, reason = PauseReason.Network))

        assertDialog(PAUSED_TITLE, "$SAVED_UNCONFIRMED\n$CHECK_NETWORK", RETRY, STOP_AND_VIEW)
    }

    @Test
    fun `저장 없이 일시 정지하면 빈 덱이라 단정하지 않고 저장됐는지 확인하지 못했다고 안내한다`() {
        show(ImportDialog.Paused(saved = 0, reason = PauseReason.Later))

        assertDialog(PAUSED_TITLE, "카드가 저장됐는지 확인하지 못했어요.\n잠시 뒤 다시 시도해 주세요.", RETRY, STOP_AND_VIEW)
    }

    @Test
    fun `결과를 확인하지 못한 키 충돌은 덱을 열어 확인하라고 안내하고 덱 보기만 둔다`() {
        show(ImportDialog.KeyConflict(saved = 1_000, isSaveUnconfirmed = true))

        assertDialog(KEY_CONFLICT_TITLE, "$SAVED_UNCONFIRMED\n덱을 열어 저장된 카드를 확인해 주세요.", "덱 보기")
        composeRule.onNodeWithText("덱 보기").performClick()
        intents shouldBe listOf(ImportDeckIntent.ClickViewDeck)
    }

    @Test
    fun `장수가 확정된 키 충돌은 저장한 장수와 덱을 볼 수 있다는 문장이다`() {
        show(ImportDialog.KeyConflict(saved = 1_002, isSaveUnconfirmed = false))

        assertDialog(KEY_CONFLICT_TITLE, "1,002장까지 저장했어요.\n저장한 카드로 덱을 볼 수 있어요.", "덱 보기")
    }

    @Test
    fun `장수가 확정된 키 충돌에서 저장한 카드가 없으면 저장 줄만 보인다`() {
        show(ImportDialog.KeyConflict(saved = 0, isSaveUnconfirmed = false))

        assertDialog(KEY_CONFLICT_TITLE, "아직 저장한 카드가 없어요.", "덱 보기")
    }

    @Test
    fun `일부 저장 실패는 저장한 장수와 다시 시도하거나 덱을 볼 수 있다는 문장이다`() {
        show(ImportDialog.PartialFailure(saved = 1_000))

        assertDialog(PARTIAL_TITLE, "1,000장까지 저장했어요.\n다시 시도하거나 저장한 카드로 덱을 볼 수 있어요.", RETRY, STOP_AND_VIEW)
    }

    @Test
    fun `저장 없이 일부 저장 실패하면 빈 덱을 볼 수 있다고 안내하고 다시 시도는 저장 다시 시도를 보낸다`() {
        show(ImportDialog.PartialFailure(saved = 0))

        assertDialog(PARTIAL_TITLE, "아직 저장한 카드가 없어요.\n다시 시도하거나 빈 덱을 볼 수 있어요.", RETRY, STOP_AND_VIEW)
        composeRule.onNodeWithText(RETRY).performClick()
        intents shouldBe listOf(ImportDeckIntent.RetrySave)
    }

    @Test
    fun `덱을 찾을 수 없으면 홈으로만 두고 홈으로는 홈 이동을 보낸다`() {
        show(ImportDialog.DeckUnavailable)

        assertDialog("덱을 찾을 수 없어요", "다른 기기에서 덱이 삭제됐을 수 있어요.", "홈으로")
        composeRule.onNodeWithText("홈으로").performClick()
        intents shouldBe listOf(ImportDeckIntent.ClickGoHome)
    }

    private companion object {
        const val RETRY = "다시 시도"
        const val STOP_AND_VIEW = "멈추고 덱 보기"
        const val CHECK_NETWORK = "네트워크를 확인하고 다시 시도해 주세요."
        const val PAUSED_TITLE = "저장이 잠시 멈췄어요"
        const val PARTIAL_TITLE = "일부 카드를 저장하지 못했어요"
        const val KEY_CONFLICT_TITLE = "카드를 더 저장할 수 없어요"
        const val SAVED_UNCONFIRMED = "1,000장까지 저장했고,\n일부 카드가 더 저장됐을 수 있어요."
    }
}
