package com.whatever.caro.feature.importer.step

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.component.isHeading
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.transform.ImportPreview
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 미리보기 단계는 Screen이 상태의 미리보기, 필터, 대화상자를 골라 넘기고 intent로 되돌려야 하므로 Screen을 통해 확인한다.
// 로딩 카드의 무한 반복 Lottie 때문에 자동 시계를 켜 두면 waitForIdle이 끝나지 않는 경우에는 시계를 직접 움직인다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class PreviewStepWiringTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val intents = mutableListOf<ImportDeckIntent>()
    private var screenState by mutableStateOf(ImportDeckState())

    private val apkgTable = MappingStepTables.table(MappingStepTables.vocab())
    private val figmaPreview = PreviewStepTables.preview()

    private fun show(
        state: ImportDeckState,
        manualClock: Boolean = false,
    ) {
        screenState = state
        if (manualClock) composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CaroTheme {
                ImportDeckScreen(state = screenState, onIntent = { intents += it })
            }
        }
        if (manualClock) composeRule.mainClock.advanceTimeByFrame() else composeRule.waitForIdle()
    }

    private fun previewState(
        preview: ImportPreview? = figmaPreview,
        table: ParsedTable = apkgTable,
        filter: ImportPreviewFilter = ImportPreviewFilter.All,
        dialog: ImportDialog? = null,
        isPaste: Boolean = false,
    ) = ImportDeckState(
        step = ImportStep.Preview,
        table = table,
        preview = preview,
        previewFilter = filter,
        dialog = dialog,
        isPaste = isPaste,
    )

    private fun verticalOffset(): Float =
        composeRule
            .onNode(hasVerticalScrollRange)
            .fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
            .value()

    private fun scrollListUp() {
        composeRule.onNode(hasVerticalScrollRange).performTouchInput { swipeUp() }
        composeRule.waitForIdle()
    }

    private fun applyState(next: ImportDeckState) {
        composeRule.runOnIdle { screenState = next }
        // 상태 변경 알림을 비운 뒤 프레임을 움직여야 다시 그려지고 포커스 요청이 실행된다.
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
    }

    @Test
    fun `미리보기 단계는 상태의 미리보기를 요약에 그린다`() {
        show(previewState())

        composeRule.onNodeWithText("가져올 카드").assertIsDisplayed()
        composeRule.onNodeWithText("8장").assertIsDisplayed()
    }

    @Test
    fun `미리보기가 아직 없으면 아무것도 그리지 않는다`() {
        show(previewState(preview = null))

        composeRule.onNodeWithText("가져올 카드").assertDoesNotExist()
    }

    @Test
    fun `미리보기 단계에서 카드를 누르면 그 카드로 카드 누름 intent를 보낸다`() {
        show(previewState())

        composeRule.onNode(cardItem(PreviewStepTables.READ)).performClick()

        intents shouldBe listOf(ImportDeckIntent.ClickCard(PreviewStepTables.READ))
    }

    @Test
    fun `미리보기 단계에서 필터 칩을 누르면 그 필터로 필터 선택 intent를 보낸다`() {
        show(previewState())

        composeRule.onNode(isTabRole and hasText("잘린 내용")).performClick()

        intents shouldBe listOf(ImportDeckIntent.SelectPreviewFilter(ImportPreviewFilter.Truncated))
    }

    @Test
    fun `미리보기 단계는 상태의 필터를 칩에 그린다`() {
        show(previewState(filter = ImportPreviewFilter.MediaRemoved))

        composeRule.onNode(isTabRole and hasText("미디어 제거")).assertIsSelected()
    }

    @Test
    fun `미리보기 단계에서 다음을 누르면 덱 정보 미리 채움 문구를 담아 다음 intent를 보낸다`() {
        show(previewState())

        composeRule.onNodeWithText("다음").performClick()

        intents shouldBe
            listOf(
                ImportDeckIntent.ClickPreviewNext(
                    fileDescriptionTemplate = "{file}에서 가져왔어요",
                    pasteDescription = "붙여 넣은 내용으로 만들었어요",
                ),
            )
    }

    @Test
    fun `엑셀 표의 미리보기가 상한을 넘으면 엑셀용 사유를 보여준다`() {
        show(previewState(preview = PreviewStepTables.preview(limitExceeded = true), table = SelectionStepTables.xlsx()))

        composeRule.onNodeWithText("한 번에 10,000장까지 가져올 수 있어요. 시트나 행을 나눠 여러 파일로 저장해 주세요.").assertIsDisplayed()
    }

    @Test
    fun `붙여넣기 상태의 미리보기가 상한을 넘으면 붙여넣기용 사유를 보여준다`() {
        show(previewState(preview = PreviewStepTables.preview(limitExceeded = true), table = TextCheckStepTables.text(), isPaste = true))

        composeRule.onNodeWithText("한 번에 10,000장까지 가져올 수 있어요. 10,000줄 이하로 나눠 붙여 넣어 주세요.").assertIsDisplayed()
    }

    @Test
    fun `카드 전체 보기 대화상자가 아니면 팝업을 그리지 않는다`() {
        show(previewState())

        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun `상태의 대화상자가 카드 전체 보기면 앞면과 뒷면 구역 팝업을 그린다`() {
        show(previewState(dialog = ImportDialog.CardDetail(PreviewStepTables.JAPAN)))

        composeRule.onNode(isDialog()).assertIsDisplayed()
        composeRule.onNode(hasText("앞면") and isHeading).assertIsDisplayed()
        composeRule.onNode(hasText("뒷면") and isHeading).assertIsDisplayed()
    }

    @Test
    fun `카드 전체 보기 팝업은 하나만 그린다`() {
        show(previewState(dialog = ImportDialog.CardDetail(PreviewStepTables.JAPAN)))

        composeRule.onAllNodes(isDialog()).assertCountEquals(1)
    }

    @Test
    fun `카드 전체 보기 팝업의 닫기를 누르면 대화상자 닫기 intent를 보낸다`() {
        show(previewState(dialog = ImportDialog.CardDetail(PreviewStepTables.JAPAN)))

        composeRule.onNode(hasText("닫기") and hasClickAction()).performClick()

        intents shouldBe listOf(ImportDeckIntent.DismissDialog)
    }

    @Test
    fun `미리보기를 만들다 끝나 미리보기 단계로 넘어오면 가져올 장수 제목에 포커스를 둔다`() {
        show(ImportDeckState(step = ImportStep.Mapping, table = apkgTable, loading = ImportLoading.BuildingPreview), manualClock = true)

        applyState(previewState())

        composeRule.onNode(hasText("8장") and isHeading).assertIsFocused()
    }

    @Test
    fun `다른 단계로 갔다 같은 미리보기로 돌아오면 목록의 스크롤 위치를 보존한다`() {
        val preview = PreviewStepTables.longPreview()
        show(previewState(preview = preview))
        scrollListUp()
        verticalOffset() shouldBeGreaterThan 0f
        val scrolled = verticalOffset()

        composeRule.runOnIdle { screenState = previewState(preview = preview).copy(step = ImportStep.Source) }
        composeRule.runOnIdle { screenState = previewState(preview = preview) }
        composeRule.waitForIdle()

        verticalOffset() shouldBe scrolled
    }

    @Test
    fun `새 미리보기가 만들어지면 목록의 스크롤 위치를 처음으로 돌린다`() {
        show(previewState(preview = PreviewStepTables.longPreview()))
        scrollListUp()
        verticalOffset() shouldBeGreaterThan 0f

        // 값이 같은 미리보기는 같은 상태로 보아 다시 그리지 않으므로, 다시 만든 미리보기는 값이 다른 미리보기여야 한다.
        composeRule.runOnIdle { screenState = previewState(preview = PreviewStepTables.longPreview(extra = 41)) }
        composeRule.waitForIdle()

        verticalOffset() shouldBe 0f
    }

    @Test
    fun `미리보기 단계에서 상태를 저장했다 복원해도 화면이 다시 그려진다`() {
        screenState = previewState(preview = PreviewStepTables.longPreview())
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            CaroTheme {
                ImportDeckScreen(state = screenState, onIntent = { intents += it })
            }
        }
        scrollListUp()
        val scrolled = verticalOffset()

        restorationTester.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithContentDescription("뒤로 가기").assertExists()
        verticalOffset() shouldBe scrolled
    }

    private companion object {
        const val SETTLE_MILLIS = 48L
    }
}
