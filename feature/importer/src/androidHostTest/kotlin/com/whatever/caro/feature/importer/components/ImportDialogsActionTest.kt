package com.whatever.caro.feature.importer.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDialog
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 읽기 실패, 카드 없음, 카드가 너무 많음은 ViewModel이 띄울 때 이미 처음이나 붙여넣기로 돌려 두므로 [확인]은 닫기만 보낸다.
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
    fun `파일이 너무 큰 대화상자의 확인은 닫기를 보낸다`() {
        showAndClick(ImportDialog.FileTooLarge(extension = "apkg"), "확인")

        intents shouldBe listOf(ImportDeckIntent.DismissDialog)
    }

    @Test
    fun `읽기 실패 대화상자의 확인은 닫기를 보낸다`() {
        showAndClick(ImportDialog.ParseError(ImportParseErrorKind.CORRUPT_FILE, extension = "apkg", isPaste = false), "확인")

        intents shouldBe listOf(ImportDeckIntent.DismissDialog)
    }
}
