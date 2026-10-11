package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.component.isHeading
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 문구는 Figma 프레임의 표시 문구다. 행 수 상한은 주입한 값을 따른다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class SheetStepTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val selectedIndexes = mutableListOf<Int>()
    private val headerChanges = mutableListOf<Boolean>()
    private var nextClicks = 0

    private val isPoliteLiveRegion = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    private fun show(
        table: ParsedTable = SelectionStepTables.xlsx(sheets = SHEETS),
        sheetIndex: Int = 0,
        hasHeaderRow: Boolean = true,
        maxCards: Int = 10_000,
        height: Dp = 700.dp,
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density = baseDensity.density, fontScale = fontScale)) {
                CaroTheme {
                    Box(modifier = Modifier.size(393.dp, height)) {
                        SheetStep(
                            table = table,
                            sheetIndex = sheetIndex,
                            hasHeaderRow = hasHeaderRow,
                            maxCards = maxCards,
                            onSelectSheet = { index -> selectedIndexes += index },
                            onChangeHeaderRow = { checked -> headerChanges += checked },
                            onNext = { nextClicks += 1 },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun row(label: String): SemanticsNodeInteraction = composeRule.onNode(hasText(label))

    private fun rowTexts(label: String): List<String> =
        row(label).fetchSemanticsNode().config[SemanticsProperties.Text].map { text -> text.text }

    private fun nextButton(): SemanticsNodeInteraction = composeRule.onNodeWithText(NEXT)

    private fun SemanticsNodeInteraction.toggleState(): ToggleableState = fetchSemanticsNode().config[SemanticsProperties.ToggleableState]

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    private fun textNode(text: String): SemanticsNodeInteraction = composeRule.onNodeWithText(text, useUnmergedTree = true)

    private fun manySheetsTable(count: Int): ParsedTable =
        SelectionStepTables.xlsx(sheets = List(count) { index -> "시트 ${index + 1}" to 100 })

    @Test
    fun `파일 이름을 부제로 보여준다`() {
        show()

        composeRule.onNodeWithText(SelectionStepTables.XLSX_NAME).assertIsDisplayed()
    }

    @Test
    fun `안내 제목을 제목으로 읽히게 한다`() {
        show()

        composeRule.onNode(hasText(HEADING) and isHeading).assertIsDisplayed()
    }

    @Test
    fun `다른 시트는 가져오지 않는다는 안내를 보여준다`() {
        show()

        composeRule.onNodeWithText(ONLY_SELECTED_HINT).assertIsDisplayed()
    }

    @Test
    fun `시트 줄은 이름과 행 수를 보여준다`() {
        show()

        rowTexts("N5 단어") shouldContain "820행"
    }

    @Test
    fun `행이 없는 시트는 행 수 대신 비어 있음을 보여준다`() {
        show()

        rowTexts("메모") shouldContain "비어 있음"
    }

    @Test
    fun `행 수가 상한을 넘는 시트는 상한 행 넘음 라벨을 보여준다`() {
        show(table = SelectionStepTables.xlsx(sheets = SHEETS_WITH_LARGE))

        rowTexts("전체 단어") shouldContain "10,000행 넘음"
    }

    @Test
    fun `행 수가 상한과 같은 시트는 넘음이 아니라 행 수를 보여준다`() {
        show(table = SelectionStepTables.xlsx(sheets = listOf("정확히 상한" to 10_000)))

        rowTexts("정확히 상한") shouldContain "10,000행"
    }

    @Test
    fun `넘음 라벨의 숫자는 주입한 상한을 따른다`() {
        show(table = SelectionStepTables.xlsx(sheets = listOf("N5 단어" to 820, "큰 시트" to 6_000)), maxCards = 5_000)

        rowTexts("큰 시트") shouldContain "5,000행 넘음"
    }

    @Test
    fun `고른 시트 줄만 선택 상태다`() {
        show(sheetIndex = 1)

        composeRule.onNode(hasText("N4 단어")).assertIsSelected()
        composeRule.onNode(hasText("N5 단어")).assertIsNotSelected()
    }

    @Test
    fun `시트 줄은 라디오 버튼 역할이다`() {
        show()

        row("N5 단어").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
    }

    @Test
    fun `시트 줄의 높이는 48dp 이상이다`() {
        show()

        row("N5 단어").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `시트 줄을 누르면 그 시트 index로 선택을 요청한다`() {
        show()

        row("N4 단어").performClick()

        selectedIndexes shouldBe listOf(1)
    }

    @Test
    fun `빈 시트를 고르면 고정 이유 줄이 보인다`() {
        show(sheetIndex = 2)

        composeRule.onNodeWithText(EMPTY_REASON).assertIsDisplayed()
    }

    @Test
    fun `빈 시트를 고르면 이유 줄이 polite live region이다`() {
        show(sheetIndex = 2)

        composeRule.onNodeWithText(EMPTY_REASON).assert(isPoliteLiveRegion)
    }

    @Test
    fun `빈 시트를 고르면 다음 버튼이 꺼진다`() {
        show(sheetIndex = 2)

        nextButton().assertIsNotEnabled()
    }

    @Test
    fun `빈 시트를 고르면 다음 버튼의 상태 설명에 이유가 들어간다`() {
        show(sheetIndex = 2)

        nextButton().fetchSemanticsNode().config[SemanticsProperties.StateDescription] shouldBe EMPTY_REASON
    }

    @Test
    fun `비어 있지 않은 시트를 고르면 이유 줄이 없고 다음 버튼이 켜진다`() {
        show(sheetIndex = 0)

        composeRule.onNodeWithText(EMPTY_REASON).assertDoesNotExist()
        nextButton().assertIsEnabled()
    }

    @Test
    fun `행 수가 상한을 넘는 시트를 골라도 다음 버튼은 켜져 있다`() {
        show(table = SelectionStepTables.xlsx(sheets = SHEETS_WITH_LARGE), sheetIndex = 2)

        nextButton().assertIsEnabled()
    }

    @Test
    fun `다음 버튼을 누르면 다음을 한 번 요청한다`() {
        show()

        nextButton().performClick()

        nextClicks shouldBe 1
    }

    @Test
    fun `첫 줄은 제목이 켜져 있으면 줄이 켜진 상태다`() {
        show(hasHeaderRow = true)

        row(HEADER_ROW).toggleState() shouldBe ToggleableState.On
    }

    @Test
    fun `첫 줄은 제목이 꺼져 있으면 줄이 꺼진 상태다`() {
        show(hasHeaderRow = false)

        row(HEADER_ROW).toggleState() shouldBe ToggleableState.Off
    }

    @Test
    fun `첫 줄은 제목 줄을 눌러 켜진 값을 끈다`() {
        show(hasHeaderRow = true)

        row(HEADER_ROW).performClick()

        headerChanges shouldBe listOf(false)
    }

    @Test
    fun `첫 줄은 제목 줄을 눌러 꺼진 값을 켠다`() {
        show(hasHeaderRow = false)

        row(HEADER_ROW).performClick()

        headerChanges shouldBe listOf(true)
    }

    @Test
    fun `첫 줄은 제목 줄은 체크박스 역할의 toggleable이고 높이가 48dp 이상이다`() {
        show()

        row(HEADER_ROW).assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
        row(HEADER_ROW).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `체크 줄의 toggleable은 첫 줄은 제목 줄 하나뿐이고 체크박스는 따로 눌리지 않는다`() {
        show()

        composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState), useUnmergedTree = true)
            .assertCountEquals(1)
    }

    @Test
    fun `긴 시트 이름은 100자 이내로 줄여 말줄임을 붙인다`() {
        show(table = SelectionStepTables.xlsx(sheets = listOf(LONG_SHEET_NAME to 820, "N4 단어" to 640)))

        val shown =
            longSheetNameNode()
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .single()
                .text

        shown.length shouldBeLessThanOrEqual 100
        shown shouldEndWith "…"
    }

    @Test
    fun `긴 시트 이름은 2줄까지 보이고 마지막 줄이 말줄임으로 끝난다`() {
        show(table = SelectionStepTables.xlsx(sheets = listOf(LONG_SHEET_NAME to 820, "N4 단어" to 640)))

        val layout = longSheetNameNode().textLayout()

        listOf(layout.lineCount, layout.isLineEllipsized(1)) shouldBe listOf(2, true)
    }

    @Test
    fun `글자 130퍼센트 미만에서는 행 수가 시트 이름과 같은 줄에 놓인다`() {
        show(fontScale = 1.25f)
        val name = textNode("N5 단어").fetchSemanticsNode().boundsInRoot
        val rowCount = textNode("820행").fetchSemanticsNode().boundsInRoot

        (name.top < rowCount.bottom && rowCount.top < name.bottom) shouldBe true
    }

    @Test
    fun `글자 130퍼센트에서는 행 수가 시트 이름 아래 줄로 내려간다`() {
        show(fontScale = 1.3f)
        val name = textNode("N5 단어").fetchSemanticsNode().boundsInRoot
        val rowCount = textNode("820행").fetchSemanticsNode().boundsInRoot

        // 줄 경계의 반올림 오차 1px는 허용한다.
        name.bottom shouldBeLessThanOrEqual (rowCount.top + 1f)
    }

    @Test
    fun `최대 글자 크기에서도 행 수는 시트 이름 아래 줄이다`() {
        show(fontScale = 2f)
        val name = textNode("N5 단어").fetchSemanticsNode().boundsInRoot
        val rowCount = textNode("820행").fetchSemanticsNode().boundsInRoot

        name.bottom shouldBeLessThanOrEqual (rowCount.top + 1f)
    }

    @Test
    fun `목록은 세로 스크롤 컨테이너 하나뿐이다`() {
        show()

        composeRule.onAllNodes(hasScrollAction()).assertCountEquals(1)
    }

    @Test
    fun `시트가 많으면 보이는 줄만 그린다`() {
        show(table = manySheetsTable(count = 200))

        val drawnRows = composeRule.onAllNodes(hasText("시트 ", substring = true)).fetchSemanticsNodes().size

        drawnRows shouldBeGreaterThan 0
        drawnRows shouldBeLessThan 200
    }

    @Test
    fun `높이가 모자라면 스크롤로 첫 줄은 제목 줄까지 닿고 다음 버튼은 계속 보인다`() {
        show(table = manySheetsTable(count = 40), height = 400.dp)

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(HEADER_ROW))

        row(HEADER_ROW).assertIsDisplayed()
        nextButton().assertIsDisplayed()
    }

    private fun longSheetNameNode(): SemanticsNodeInteraction =
        composeRule.onNode(hasText(LONG_SHEET_NAME_PREFIX, substring = true), useUnmergedTree = true)

    private companion object {
        const val HEADING = "가져올 시트를 골라 주세요"
        const val ONLY_SELECTED_HINT = "다른 시트는 가져오지 않아요."
        const val HEADER_ROW = "첫 줄은 제목"
        const val EMPTY_REASON = "비어 있는 시트예요. 다른 시트를 골라 주세요."
        const val NEXT = "다음"
        const val LONG_SHEET_NAME_PREFIX = "긴시트이름긴시트이름"
        val LONG_SHEET_NAME = "긴시트이름".repeat(60)
        val SHEETS = listOf("N5 단어" to 820, "N4 단어" to 640, "메모" to 0)
        val SHEETS_WITH_LARGE = listOf("N5 단어" to 820, "N4 단어" to 640, "전체 단어" to 10_001, "메모" to 0)
    }
}
