package com.whatever.caro.feature.importer.components

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.feature.importer.logic.NoCardsKind
import com.whatever.caro.feature.importer.mvi.ImportDialog
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 문구는 Figma 표시 문구다. 본문이 두 문장이면 줄바꿈 하나로 잇고, 버튼은 하려는 동작이 왼쪽이다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class ImportDialogsCopyTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(dialog: ImportDialog) {
        composeRule.showImportDialog(dialog)
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
    fun `apkg 파일이 너무 크면 크기 안내와 Anki 안내를 보여준다`() {
        show(ImportDialog.FileTooLarge(extension = "apkg"))

        assertDialog("파일이 너무 커요", "$SIZE_LIMIT\nAnki에서 미디어를 빼거나 하위 덱별로 나눠 내보내 주세요.", CONFIRM)
    }

    @Test
    fun `xlsx 파일이 너무 크면 크기 안내와 엑셀 안내를 보여준다`() {
        show(ImportDialog.FileTooLarge(extension = "xlsx"))

        assertDialog("파일이 너무 커요", "$SIZE_LIMIT\n시트나 줄을 나눠 여러 파일로 저장해 주세요.", CONFIRM)
    }

    @Test
    fun `텍스트 파일이 너무 크면 크기 안내와 파일을 나누라는 안내를 보여준다`() {
        show(ImportDialog.FileTooLarge(extension = "csv"))

        assertDialog("파일이 너무 커요", "$SIZE_LIMIT\n$SPLIT_FILE", CONFIRM)
    }

    @Test
    fun `xls 파일은 xlsx로 다시 저장하라는 안내를 보여준다`() {
        show(ImportDialog.ParseError(ImportParseErrorKind.UNSUPPORTED_FILE, extension = "xls", isPaste = false))

        assertDialog("가져올 수 없는 파일이에요", "예전 엑셀 파일(.xls)이에요.\n엑셀에서 .xlsx로 다시 저장해 주세요.", CONFIRM)
    }

    @Test
    fun `지원하지 않는 xlsx 파일은 암호와 다시 저장 안내를 보여준다`() {
        show(ImportDialog.ParseError(ImportParseErrorKind.UNSUPPORTED_FILE, extension = "xlsx", isPaste = false))

        assertDialog("가져올 수 없는 파일이에요", "암호가 걸려 있다면 풀고, 엑셀에서 .xlsx로 다시 저장해 주세요.", CONFIRM)
    }

    @Test
    fun `그 밖의 확장자는 지원하는 파일 목록 안내를 보여준다`() {
        show(ImportDialog.ParseError(ImportParseErrorKind.UNSUPPORTED_FILE, extension = "pdf", isPaste = false))

        assertDialog("가져올 수 없는 파일이에요", "지원하는 파일(.apkg, .txt, .csv, .tsv, .xlsx)을 골라 주세요.", CONFIRM)
    }

    @Test
    fun `손상된 apkg 파일은 손상 안내와 다시 내보내기 안내를 보여준다`() {
        show(ImportDialog.ParseError(ImportParseErrorKind.CORRUPT_FILE, extension = "apkg", isPaste = false))

        assertDialog("파일을 열 수 없어요", "파일이 손상됐을 수 있어요.\nAnki에서 다시 내보내 주세요.", CONFIRM)
    }

    @Test
    fun `손상된 xlsx 파일은 손상 안내와 다시 저장 안내를 보여준다`() {
        show(ImportDialog.ParseError(ImportParseErrorKind.CORRUPT_FILE, extension = "xlsx", isPaste = false))

        assertDialog("파일을 열 수 없어요", "파일이 손상됐을 수 있어요.\n엑셀에서 다시 저장해 주세요.", CONFIRM)
    }

    @Test
    fun `글자 인코딩을 정하지 못하면 UTF-8 저장 안내를 보여준다`() {
        show(ImportDialog.ParseError(ImportParseErrorKind.ENCODING_UNDETERMINED, extension = "txt", isPaste = false))

        assertDialog("글자를 읽을 수 없어요", "파일을 UTF-8로 저장한 뒤 다시 골라 주세요.", CONFIRM)
    }

    @Test
    fun `파일을 읽지 못하면 나눠 다시 고르라는 안내를 보여준다`() {
        show(ImportDialog.ParseError(ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED, extension = "txt", isPaste = false))

        assertDialog("파일을 읽지 못했어요", "내용이 너무 많거나 파일이 손상됐을 수 있어요.\n$SPLIT_FILE", CONFIRM)
    }

    @Test
    fun `열이 너무 많은 파일은 필요한 열만 남기라는 안내를 보여준다`() {
        show(ImportDialog.ParseError(ImportParseErrorKind.TOO_MANY_COLUMNS, extension = "csv", isPaste = false))

        assertDialog("파일을 읽지 못했어요", "열이 너무 많아요.\n필요한 열만 남겨 다시 저장해 주세요.", CONFIRM)
    }

    @Test
    fun `붙여 넣은 내용을 읽지 못하면 나눠 다시 붙여 넣으라는 안내를 보여준다`() {
        show(ImportDialog.ParseError(ImportParseErrorKind.CONTENT_TOO_LONG, extension = "", isPaste = true))

        assertDialog("내용을 읽지 못했어요", "내용을 나눠 다시 붙여 넣어 주세요.", CONFIRM)
    }

    @Test
    fun `카드로 만들 내용이 없으면 내용을 확인하라는 안내를 보여준다`() {
        show(ImportDialog.NoCards(NoCardsKind.NoContent))

        assertDialog("가져올 카드가 없어요", "카드로 만들 내용이 있는지 확인해 주세요.", CONFIRM)
    }

    @Test
    fun `Image Occlusion 카드만 있으면 전용 본문을 보여준다`() {
        show(ImportDialog.NoCards(NoCardsKind.ImageOcclusionOnly))

        assertDialog("가져올 카드가 없어요", "Image Occlusion 카드는 가져올 수 없어서, 이 파일에는 가져올 카드가 없어요.", CONFIRM)
    }

    @Test
    fun `파일의 카드가 너무 많으면 상한과 파일을 나누라는 안내를 보여준다`() {
        show(ImportDialog.TooManyCards(isPaste = false))

        assertDialog(TOO_MANY_TITLE, "$CARD_LIMIT\n$SPLIT_FILE", CONFIRM)
    }

    @Test
    fun `붙여 넣은 카드가 너무 많으면 상한과 나눠 붙여 넣으라는 안내를 보여준다`() {
        show(ImportDialog.TooManyCards(isPaste = true))

        assertDialog(TOO_MANY_TITLE, "$CARD_LIMIT\n내용을 나눠 여러 번 붙여 넣어 주세요.", CONFIRM)
    }

    @Test
    fun `처음으로 돌아갈지 묻는 대화상자는 돌아가기가 왼쪽 취소가 오른쪽이다`() {
        show(ImportDialog.DiscardConfirm)

        assertDialog("처음으로 돌아갈까요?", "읽은 파일과 고른 설정이 사라져요.", "돌아가기", "취소")
    }

    private companion object {
        const val CONFIRM = "확인"
        const val SIZE_LIMIT = "100MB까지 가져올 수 있어요."
        const val SPLIT_FILE = "파일을 나눠 다시 골라 주세요."
        const val TOO_MANY_TITLE = "카드가 너무 많아요"
        const val CARD_LIMIT = "한 번에 10,000장까지 가져올 수 있어요."
    }
}
