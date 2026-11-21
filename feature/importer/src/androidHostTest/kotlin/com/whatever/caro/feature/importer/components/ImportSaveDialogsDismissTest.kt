package com.whatever.caro.feature.importer.components

import androidx.activity.ComponentDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import com.whatever.caro.core.designsystem.themes.CaroTheme
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
import org.robolectric.shadows.ShadowDialog

// 저장이 멈춘 뒤의 대화상자는 고를 때까지 남아야 하고, 덱 만들기 실패와 멈춤 확인은 바깥 누름과 뒤로 가기가 머무는 버튼과 같다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class ImportSaveDialogsDismissTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<ImportDeckIntent>()
    private var shown by mutableStateOf<ImportDialog?>(null)

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

    @Test
    fun `저장이 멈춘 뒤의 대화상자는 창이 바깥 누름과 뒤로 가기를 받지 않는다`() {
        val recording = RecordingDialogFrame()
        showSwitchable(recording)

        val flags =
            FIXED_DIALOGS.map { dialog ->
                switchTo(dialog)
                recording.properties!!.let { properties -> properties.dismissOnBackPress to properties.dismissOnClickOutside }
            }

        flags shouldBe List(FIXED_DIALOGS.size) { false to false }
    }

    @Test
    fun `저장이 멈춘 뒤의 대화상자는 닫기 요청이 와도 intent를 보내지 않는다`() {
        val recording = RecordingDialogFrame()
        showSwitchable(recording)

        FIXED_DIALOGS.forEach { dialog ->
            switchTo(dialog)
            composeRule.runOnIdle { recording.onDismissRequest!!.invoke() }
        }

        intents shouldBe emptyList()
    }

    @Test
    fun `덱 만들기 실패와 멈춤 확인은 바깥 누름과 뒤로 가기를 받아 머무는 버튼과 같은 닫기를 보낸다`() {
        val recording = RecordingDialogFrame()
        showSwitchable(recording)

        val sent =
            listOf(ImportDialog.DeckCreateFailed, ImportDialog.StopConfirm).map { dialog ->
                switchTo(dialog)
                intents.clear()
                composeRule.runOnIdle { recording.onDismissRequest!!.invoke() }
                Triple(recording.properties!!.dismissOnBackPress, recording.properties!!.dismissOnClickOutside, intents.toList())
            }

        sent shouldBe List(2) { Triple(true, true, listOf(ImportDeckIntent.DismissDialog)) }
    }

    @Test
    fun `실제 창에서 일시 정지 대화상자에 뒤로 가기를 눌러도 intent를 보내지 않고 대화상자가 남는다`() {
        composeRule.setContent {
            CaroTheme {
                ImportDialogs(dialog = ImportDialog.Paused(saved = 1_000, reason = PauseReason.Network), onIntent = { intents += it })
            }
        }
        composeRule.onNode(isDialog()).assertExists()

        composeRule.runOnIdle { (ShadowDialog.getLatestDialog() as ComponentDialog).onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        intents shouldBe emptyList()
        composeRule.onNode(isDialog()).assertExists()
    }

    private companion object {
        val FIXED_DIALOGS =
            listOf(
                ImportDialog.Paused(saved = 1_000, reason = PauseReason.Network),
                ImportDialog.Paused(saved = 0, reason = PauseReason.Later),
                ImportDialog.PartialFailure(saved = 1_000),
                ImportDialog.KeyConflict(saved = 1_000, isSaveUnconfirmed = true),
                ImportDialog.DeckUnavailable,
            )
    }
}
