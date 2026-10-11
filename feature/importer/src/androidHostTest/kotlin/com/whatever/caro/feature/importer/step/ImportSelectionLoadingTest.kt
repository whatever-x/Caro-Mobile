package com.whatever.caro.feature.importer.step

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.component.isHeading
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportSelection
import com.whatever.caro.feature.importer.mvi.ImportStep
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 로딩 카드의 무한 반복 Lottie 때문에 자동 시계를 켜 두면 waitForIdle이 끝나지 않으므로 시계를 직접 움직인다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class ImportSelectionLoadingTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var screenState by mutableStateOf(ImportDeckState())

    private fun showScreen(initial: ImportDeckState) {
        screenState = initial
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CaroTheme {
                ImportDeckScreen(state = screenState, onIntent = {})
            }
        }
        composeRule.mainClock.advanceTimeBy(FRAME_MILLIS)
    }

    private fun applyState(next: ImportDeckState) {
        composeRule.runOnIdle { screenState = next }
        // 상태 변경 알림을 비운 뒤 프레임을 움직여야 다시 그려지고 포커스 요청이 실행된다.
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
    }

    private fun subdeckStep(loading: ImportLoading? = null) =
        ImportDeckState(
            step = ImportStep.SubdeckSelect,
            table = SelectionStepTables.apkg(),
            selection = ImportSelection(subdeckIds = persistentSetOf("1", "2")),
            loading = loading,
        )

    private fun sheetStep(loading: ImportLoading? = null) =
        ImportDeckState(
            step = ImportStep.SheetSelect,
            table = SelectionStepTables.xlsx(),
            selection = ImportSelection(hasHeaderRow = true),
            loading = loading,
        )

    @Test
    fun `고른 하위 덱을 읽는 중에는 하위 덱 선택을 그려 둔 채 그 위에 파일 이름과 읽는다는 문구를 보여준다`() {
        showScreen(subdeckStep(loading = ImportLoading.Reparsing(fileName = SelectionStepTables.APKG_NAME)))

        composeRule.mainClock.advanceTimeBy(CARD_DELAY_MILLIS)

        composeRule.onNodeWithText("파일을 읽고 있어요").assertIsDisplayed()
        composeRule.onNodeWithText(SelectionStepTables.APKG_NAME).assertIsDisplayed()
        // 읽는 중에는 목록 semantics가 비워지므로 병합하지 않은 트리로 목록이 아래에 있는지 본다.
        composeRule.onNodeWithText(SUBDECK_HEADING, useUnmergedTree = true).assertExists()
    }

    @Test
    fun `하위 덱 선택으로 넘어오며 읽기가 끝나면 하위 덱 선택 제목에 포커스를 둔다`() {
        showScreen(ImportDeckState(step = ImportStep.Source, loading = ImportLoading.Parsing(fileName = SelectionStepTables.APKG_NAME)))

        applyState(subdeckStep())

        composeRule.onNode(hasText(SUBDECK_HEADING) and isHeading).assertIsFocused()
    }

    @Test
    fun `시트 선택으로 넘어오며 읽기가 끝나면 시트 선택 제목에 포커스를 둔다`() {
        showScreen(ImportDeckState(step = ImportStep.Source, loading = ImportLoading.Parsing(fileName = SelectionStepTables.XLSX_NAME)))

        applyState(sheetStep())

        composeRule.onNode(hasText(SHEET_HEADING) and isHeading).assertIsFocused()
    }

    @Test
    fun `시트를 다시 읽고 끝나면 시트 선택 제목에 포커스를 둔다`() {
        showScreen(sheetStep(loading = ImportLoading.Reparsing(fileName = SelectionStepTables.XLSX_NAME)))

        applyState(sheetStep())

        composeRule.onNode(hasText(SHEET_HEADING) and isHeading).assertIsFocused()
    }

    private companion object {
        const val SUBDECK_HEADING = "가져올 하위 덱을 골라 주세요"
        const val SHEET_HEADING = "가져올 시트를 골라 주세요"
        const val FRAME_MILLIS = 16L
        const val SETTLE_MILLIS = 48L
        const val CARD_DELAY_MILLIS = 250L
    }
}
