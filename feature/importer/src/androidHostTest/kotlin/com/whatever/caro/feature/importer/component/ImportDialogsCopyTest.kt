package com.whatever.caro.feature.importer.component

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.PauseReason
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 문구는 Figma 표시 문구다. 버튼은 하려는 동작이 왼쪽, 머무는 쪽이 오른쪽이다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class ImportDialogsCopyTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(dialog: ImportDialog) {
        composeRule.showImportDialog(dialog)
    }

    private fun assertTitle(title: String) {
        composeRule.titleNode().assertTextEquals(title)
    }

    private fun assertBodyLines(vararg lines: String) {
        lines.forEach { line -> composeRule.onNode(hasText(line, substring = true)).assertExists() }
    }

    private fun assertButtons(vararg labels: String) {
        composeRule.buttonLabelsLeftToRight() shouldBe labels.toList()
    }

    @Test
    fun `apkg 파일이 너무 크면 크기 안내와 apkg 안내를 보여준다`() {
        show(ImportDialog.FileTooLarge(extension = "apkg"))

        assertTitle("파일이 너무 커요")
        assertBodyLines("100MB까지 가져올 수 있어요.", "Anki에서 미디어를 빼거나 하위 덱별로 나눠 내보내 주세요.")
        assertButtons("확인")
    }

    @Test
    fun `xlsx 파일이 너무 크면 크기 안내와 xlsx 안내를 보여준다`() {
        show(ImportDialog.FileTooLarge(extension = "xlsx"))

        assertBodyLines("100MB까지 가져올 수 있어요.", "시트나 행을 나눠 여러 파일로 저장해 주세요.")
    }

    @Test
    fun `텍스트 파일이 너무 크면 크기 안내와 텍스트 안내를 보여준다`() {
        show(ImportDialog.FileTooLarge(extension = "csv"))

        assertBodyLines("100MB까지 가져올 수 있어요.", "파일을 나눠 다시 골라 주세요.")
    }

    @Test
    fun `xls 파일은 xlsx로 저장하라는 안내를 보여준다`() {
        show(ImportDialog.ParseError(ImportParseErrorKind.UNSUPPORTED_FILE, extension = "xls", isPaste = false))

        assertTitle("가져올 수 없는 파일이에요")
        assertBodyLines("Excel에서 .xlsx로 저장해 주세요.")
        assertButtons("확인")
    }

    @Test
    fun `지원하지 않는 xlsx 파일은 암호와 다시 저장 안내를 보여준다`() {
        show(ImportDialog.ParseError(ImportParseErrorKind.UNSUPPORTED_FILE, extension = "xlsx", isPaste = false))

        assertBodyLines("암호가 걸려 있다면 풀고, Excel에서 .xlsx로 다시 저장해 주세요.")
    }

    @Test
    fun `지원하지 않는 apkg 파일은 이전 버전 호환 안내를 보여준다`() {
        show(ImportDialog.ParseError(ImportParseErrorKind.UNSUPPORTED_FILE, extension = "apkg", isPaste = false))

        assertBodyLines("Anki 내보내기에서 이전 버전 호환 옵션을 켜고 다시 내보내 주세요.")
    }

    @Test
    fun `그 밖의 확장자는 지원하는 파일 목록 안내를 보여준다`() {
        show(ImportDialog.ParseError(ImportParseErrorKind.UNSUPPORTED_FILE, extension = "pdf", isPaste = false))

        assertBodyLines("지원하는 파일(.apkg, .txt, .csv, .tsv, .xlsx)을 골라 주세요.")
    }

    @Test
    fun `손상된 apkg 파일은 파일을 열 수 없다는 안내와 다시 내보내기 안내를 보여준다`() {
        show(ImportDialog.ParseError(ImportParseErrorKind.CORRUPT_FILE, extension = "apkg", isPaste = false))

        assertTitle("파일을 열 수 없어요")
        assertBodyLines("파일이 손상되었을 수 있어요.", "Anki에서 다시 내보내 주세요.")
        assertButtons("확인")
    }

    @Test
    fun `손상된 xlsx 파일은 파일을 열 수 없다는 안내와 다시 저장 안내를 보여준다`() {
        show(ImportDialog.ParseError(ImportParseErrorKind.CORRUPT_FILE, extension = "xlsx", isPaste = false))

        assertBodyLines("파일이 손상되었을 수 있어요.", "Excel에서 다시 저장해 주세요.")
    }

    @Test
    fun `글자 인코딩을 정하지 못하면 UTF-8 저장 안내를 보여준다`() {
        show(ImportDialog.ParseError(ImportParseErrorKind.ENCODING_UNDETERMINED, extension = "txt", isPaste = false))

        assertTitle("글자를 읽을 수 없어요")
        assertBodyLines("파일을 UTF-8로 저장한 뒤 다시 골라 주세요.")
        assertButtons("확인")
    }

    @Test
    fun `파일을 읽지 못하면 나눠 다시 고르라는 안내를 보여준다`() {
        show(ImportDialog.ParseError(ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED, extension = "txt", isPaste = false))

        assertTitle("파일을 읽지 못했어요")
        assertBodyLines("파일을 나눠 다시 골라 주세요.")
        assertButtons("확인")
    }

    @Test
    fun `열이 너무 많은 파일은 필요한 열만 남기라는 안내를 보여준다`() {
        show(ImportDialog.ParseError(ImportParseErrorKind.TOO_MANY_COLUMNS, extension = "csv", isPaste = false))

        assertTitle("파일을 읽지 못했어요")
        assertBodyLines("열이 너무 많아요. 필요한 열만 남겨 다시 저장해 주세요.")
    }

    @Test
    fun `붙여넣은 내용을 읽지 못하면 나눠 다시 붙여 넣으라는 안내를 보여준다`() {
        show(ImportDialog.ParseError(ImportParseErrorKind.CONTENT_TOO_LONG, extension = "", isPaste = true))

        assertTitle("내용을 읽지 못했어요")
        assertBodyLines("내용을 나눠 다시 붙여 넣어 주세요.")
        assertButtons("확인")
    }

    @Test
    fun `가져올 카드가 없으면 내용을 확인하라는 안내를 보여준다`() {
        show(ImportDialog.EmptyResult(isPaste = false))

        assertTitle("가져올 카드가 없어요")
        assertBodyLines("카드로 만들 내용이 있는지 확인해 주세요.")
        assertButtons("확인")
    }

    @Test
    fun `미리보기를 만들지 못하면 잠시 뒤 다시 시도하라는 안내를 보여준다`() {
        show(ImportDialog.PreviewFailed)

        assertTitle("미리보기를 만들지 못했어요")
        assertBodyLines("잠시 뒤 다시 시도해 주세요.")
        assertButtons("확인")
    }

    @Test
    fun `가져온 내용 버리기 팝업은 버리기가 왼쪽 취소가 오른쪽이다`() {
        show(ImportDialog.DiscardConfirm)

        assertTitle("가져온 내용을 버릴까요?")
        assertBodyLines("자료 가져오기로 돌아가면 읽은 내용이 사라져요.")
        assertButtons("버리기", "취소")
    }

    @Test
    fun `덱 생성 실패 팝업은 다시 시도가 왼쪽 취소가 오른쪽이다`() {
        show(ImportDialog.DeckCreateFailed)

        assertTitle("덱을 만들지 못했어요")
        assertBodyLines("네트워크를 확인하고 다시 시도해 주세요.")
        assertButtons("다시 시도", "취소")
    }

    @Test
    fun `가져오기 멈춤 확인 팝업은 멈추기가 왼쪽 계속 가져오기가 오른쪽이다`() {
        show(ImportDialog.StopConfirm)

        assertTitle("가져오기를 멈출까요?")
        assertBodyLines("저장 중인 카드까지만 덱에 남고, 나머지는 이어서 가져올 수 없어요.")
        assertButtons("멈추기", "계속 가져오기")
    }

    @Test
    fun `네트워크 때문에 멈춘 팝업은 저장한 장수와 네트워크 안내를 보여준다`() {
        show(ImportDialog.Paused(saved = 1_000, reason = PauseReason.NETWORK))

        assertTitle("저장이 잠시 멈췄어요")
        assertBodyLines("1,000장까지 저장했어요.", "네트워크를 확인하고 다시 시도해 주세요.")
        assertButtons("다시 시도", "멈추고 덱 보기")
    }

    @Test
    fun `네트워크 외 이유로 멈춘 팝업은 잠시 뒤 다시 시도하라는 안내를 보여준다`() {
        show(ImportDialog.Paused(saved = 1_000, reason = PauseReason.LATER))

        assertBodyLines("1,000장까지 저장했어요.", "잠시 뒤 다시 시도해 주세요.")
    }

    @Test
    fun `저장한 카드가 없을 때 멈춘 팝업은 저장한 카드가 없다는 문장을 보여준다`() {
        show(ImportDialog.Paused(saved = 0, reason = PauseReason.NETWORK))

        composeRule.textContaining("아직 저장한 카드가 없어요.") shouldBe "아직 저장한 카드가 없어요.\n네트워크를 확인하고 다시 시도해 주세요."
    }

    @Test
    fun `일부 카드를 저장하지 못한 팝업은 다시 시도와 멈추고 덱 보기를 보여준다`() {
        show(ImportDialog.PartialFailure(saved = 1_000))

        assertTitle("일부 카드를 저장하지 못했어요")
        assertBodyLines("1,000장까지 저장했어요.", "다시 시도하거나 저장한 카드로 덱을 볼 수 있어요.")
        assertButtons("다시 시도", "멈추고 덱 보기")
    }

    @Test
    fun `저장한 카드가 없을 때 일부 실패 팝업은 빈 덱 안내를 보여준다`() {
        show(ImportDialog.PartialFailure(saved = 0))

        composeRule.textContaining("아직 저장한 카드가 없어요.") shouldBe "아직 저장한 카드가 없어요.\n다시 시도하거나 빈 덱을 볼 수 있어요."
    }

    @Test
    fun `카드를 더 저장할 수 없는 팝업은 덱 보기 버튼 하나만 보여준다`() {
        show(ImportDialog.KeyConflict(saved = 1_000))

        assertTitle("카드를 더 저장할 수 없어요")
        assertBodyLines("1,000장까지 저장했어요.", "저장한 카드로 덱을 볼 수 있어요.")
        assertButtons("덱 보기")
    }

    @Test
    fun `저장한 카드가 없을 때 더 저장할 수 없는 팝업은 빈 덱을 지울 수 있다는 안내를 보여준다`() {
        show(ImportDialog.KeyConflict(saved = 0))

        composeRule.textContaining("아직 저장한 카드가 없어요.") shouldBe "아직 저장한 카드가 없어요.\n빈 덱은 덱 상세에서 지울 수 있어요."
    }

    @Test
    fun `덱을 찾을 수 없는 팝업은 홈으로 버튼 하나만 보여준다`() {
        show(ImportDialog.DeckUnavailable)

        assertTitle("덱을 찾을 수 없어요")
        assertBodyLines("다른 기기에서 덱이 삭제됐을 수 있어요.")
        assertButtons("홈으로")
    }
}
