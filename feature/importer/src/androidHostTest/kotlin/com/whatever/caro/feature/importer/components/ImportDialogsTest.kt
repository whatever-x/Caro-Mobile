package com.whatever.caro.feature.importer.components

import androidx.activity.ComponentDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.feature.importer.logic.NoCardsKind
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDialog
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

// 창 속성이 바깥 누름과 뒤로 가기를 닫기 요청으로 넘기는지와, 그 닫기 요청이 [확인]이나 [취소]와 같은 intent를 보내는지 나눠 확인한다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class ImportDialogsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<ImportDeckIntent>()
    private var shown by mutableStateOf<ImportDialog?>(null)

    private fun show(dialog: ImportDialog?): RecordingDialogFrame = composeRule.showImportDialog(dialog, onIntent = { intents += it })

    private fun showSwitchable(recording: RecordingDialogFrame) {
        composeRule.setContent {
            CaroTheme {
                ImportDialogs(dialog = shown, onIntent = { intents += it }, frame = recording.frame)
            }
        }
    }

    private fun switchTo(dialog: ImportDialog) {
        composeRule.runOnIdle { shown = dialog }
        composeRule.waitForIdle()
    }

    private fun showInWindow(dialog: ImportDialog) {
        composeRule.setContent {
            CaroTheme {
                ImportDialogs(dialog = dialog, onIntent = { intents += it })
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `버튼이 하나인 대화상자는 닫기 요청을 확인과 같은 intent로 보낸다`() {
        val recording = RecordingDialogFrame()
        showSwitchable(recording)

        val sent =
            ONE_BUTTON_DIALOGS.map { dialog ->
                switchTo(dialog)
                intents.clear()
                composeRule.runOnIdle { recording.onDismissRequest!!.invoke() }
                intents.toList()
            }

        sent shouldBe List(ONE_BUTTON_DIALOGS.size) { listOf(ImportDeckIntent.DismissDialog) }
    }

    @Test
    fun `버튼이 하나인 대화상자는 바깥 누름과 뒤로 가기를 닫기 요청으로 받는다`() {
        val recording = RecordingDialogFrame()
        showSwitchable(recording)

        val flags =
            ONE_BUTTON_DIALOGS.map { dialog ->
                switchTo(dialog)
                recording.properties!!.let { properties -> properties.dismissOnBackPress to properties.dismissOnClickOutside }
            }

        flags shouldBe List(ONE_BUTTON_DIALOGS.size) { true to true }
    }

    @Test
    fun `처음으로 돌아갈지 묻는 대화상자는 닫기 요청을 취소와 같은 intent로 보낸다`() {
        val recording = show(ImportDialog.DiscardConfirm)

        composeRule.runOnIdle { recording.onDismissRequest!!.invoke() }

        intents shouldBe listOf(ImportDeckIntent.DismissDialog)
    }

    @Test
    fun `처음으로 돌아갈지 묻는 대화상자도 바깥 누름과 뒤로 가기를 닫기 요청으로 받는다`() {
        val properties = show(ImportDialog.DiscardConfirm).properties!!

        listOf(properties.dismissOnBackPress, properties.dismissOnClickOutside) shouldBe listOf(true, true)
    }

    @Test
    fun `실제 대화상자 창에서 뒤로 가기를 누르면 확인과 같은 intent를 보낸다`() {
        showInWindow(ImportDialog.TooManyCards(isPaste = true))
        composeRule.onNode(isDialog()).assertExists()

        composeRule.runOnIdle { (ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        intents shouldBe listOf(ImportDeckIntent.DismissDialog)
    }

    @Test
    fun `제목은 heading으로 노출된다`() {
        show(ImportDialog.NoCards(NoCardsKind.NoContent))

        composeRule.titleNode().assertTextEquals("가져올 카드가 없어요")
    }

    @Test
    fun `대화상자가 열리면 제목에 포커스를 둔다`() {
        show(ImportDialog.DiscardConfirm)

        composeRule.titleNode().assertIsFocused()
    }

    @Test
    fun `실제 대화상자 창에서도 열리면 제목에 포커스를 둔다`() {
        showInWindow(ImportDialog.DiscardConfirm)

        composeRule.onNode(isDialog()).assertExists()
        composeRule.titleNode().assertIsFocused()
    }

    private companion object {
        val ONE_BUTTON_DIALOGS =
            listOf(
                ImportDialog.FileTooLarge(extension = "apkg"),
                ImportDialog.ParseError(ImportParseErrorKind.CORRUPT_FILE, extension = "xlsx", isPaste = false),
                ImportDialog.ParseError(ImportParseErrorKind.CONTENT_TOO_LONG, extension = "", isPaste = true),
                ImportDialog.NoCards(NoCardsKind.NoContent),
                ImportDialog.NoCards(NoCardsKind.ImageOcclusionOnly),
                ImportDialog.TooManyCards(isPaste = false),
                ImportDialog.TooManyCards(isPaste = true),
            )
    }
}
