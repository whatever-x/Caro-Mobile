package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportSelection
import com.whatever.caro.feature.importer.mvi.ImportStep
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 같은 단계 안에서 다시 읽어 표 객체가 바뀌어도 그 단계의 스크롤과 포커스는 남고, 새 자료를 읽거나 연결 입력이 바뀔 때만 처음으로 돌아가야 한다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class ImportDeckScreenStepStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var screenState by mutableStateOf(ImportDeckState())

    private fun show(
        state: ImportDeckState,
        height: Int = 852,
    ) {
        screenState = state
        composeRule.setContent {
            CaroTheme {
                Box(modifier = Modifier.size(393.dp, height.dp)) {
                    ImportDeckScreen(state = screenState, onIntent = {})
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun update(next: ImportDeckState) {
        composeRule.runOnIdle { screenState = next }
        composeRule.waitForIdle()
    }

    private fun verticalOffset(): Float =
        composeRule
            .onNode(hasVerticalScrollRange)
            .fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
            .value()

    private fun horizontalOffset(): Float =
        composeRule
            .onNode(hasHorizontalScrollRange)
            .fetchSemanticsNode()
            .config[SemanticsProperties.HorizontalScrollAxisRange]
            .value()

    private fun scrollListUp() {
        composeRule.onNode(hasVerticalScrollRange).performTouchInput { swipeUp() }
        composeRule.waitForIdle()
    }

    private fun scrollTableLeft() {
        composeRule.onNode(hasHorizontalScrollRange).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
    }

    private fun longXlsx(): ParsedTable = SelectionStepTables.xlsx(sheets = List(SHEET_COUNT) { index -> "시트 $index" to 100 })

    private fun sheetState(
        table: ParsedTable,
        sheetIndex: Int = 0,
        sourceVersion: Int = 1,
        mappingInputVersion: Int = 1,
    ) = ImportDeckState(
        step = ImportStep.SheetSelect,
        table = table,
        selection = ImportSelection(sheetIndex = sheetIndex, hasHeaderRow = true),
        sourceVersion = sourceVersion,
        mappingInputVersion = mappingInputVersion,
    )

    private fun textState(
        table: ParsedTable,
        delimiter: Char? = null,
        sourceVersion: Int = 1,
        mappingInputVersion: Int = 1,
    ) = ImportDeckState(
        step = ImportStep.TextCheck,
        table = table,
        selection = ImportSelection(delimiter = delimiter),
        sourceVersion = sourceVersion,
        mappingInputVersion = mappingInputVersion,
    )

    private fun wideText(delimiter: String = "\t"): ParsedTable =
        TextCheckStepTables.text(fieldNames = TextCheckStepTables.HEADER_NAMES, delimiter = delimiter)

    private fun longApkg(): ParsedTable = SelectionStepTables.apkg(subdecks = List(SUBDECK_COUNT) { index -> "덱 $index" to 10 })

    @Test
    fun `시트를 다시 읽어 표가 바뀌어도 시트 목록의 스크롤 위치를 보존한다`() {
        show(sheetState(longXlsx()))
        scrollListUp()
        verticalOffset() shouldBeGreaterThan 0f
        val scrolled = verticalOffset()

        update(sheetState(longXlsx(), sheetIndex = 3, mappingInputVersion = 2))

        verticalOffset() shouldBe scrolled
    }

    @Test
    fun `새 자료를 읽으면 시트 목록의 스크롤 위치를 처음으로 돌린다`() {
        show(sheetState(longXlsx()))
        scrollListUp()
        verticalOffset() shouldBeGreaterThan 0f

        update(ImportDeckState())
        update(sheetState(longXlsx(), sourceVersion = 2, mappingInputVersion = 2))

        verticalOffset() shouldBe 0f
    }

    @Test
    fun `구분자를 바꿔 다시 읽어도 텍스트 확인의 세로 스크롤 위치를 보존한다`() {
        show(textState(wideText()), height = SHORT_HEIGHT)
        scrollListUp()
        verticalOffset() shouldBeGreaterThan 0f
        val scrolled = verticalOffset()

        update(textState(wideText(delimiter = ","), delimiter = ',', mappingInputVersion = 2))

        verticalOffset() shouldBe scrolled
    }

    @Test
    fun `구분자를 바꿔 다시 읽으면 결과 표의 가로 위치만 처음으로 돌린다`() {
        show(textState(wideText()), height = SHORT_HEIGHT)
        scrollTableLeft()
        horizontalOffset() shouldBeGreaterThan 0f

        update(textState(wideText(delimiter = ","), delimiter = ',', mappingInputVersion = 2))

        horizontalOffset() shouldBe 0f
    }

    @Test
    fun `구분자를 바꿔 다시 읽어도 방금 누른 칩의 노드가 그대로 남는다`() {
        show(textState(wideText()), height = SHORT_HEIGHT)
        val before = composeRule.onNode(hasText("쉼표")).fetchSemanticsNode().id

        update(textState(wideText(delimiter = ","), delimiter = ',', mappingInputVersion = 2))

        composeRule.onNode(hasText("쉼표")).fetchSemanticsNode().id shouldBe before
    }

    @Test
    fun `항목 연결에서 돌아와도 하위 덱 목록의 스크롤 위치를 보존한다`() {
        val summary = longApkg()
        val reread = summary.copy(sourceName = "다시 읽음.apkg")
        val subdeck = ImportDeckState(step = ImportStep.SubdeckSelect, table = summary, sourceVersion = 1, mappingInputVersion = 1)
        show(subdeck)
        scrollListUp()
        verticalOffset() shouldBeGreaterThan 0f
        val scrolled = verticalOffset()

        update(ImportDeckState(step = ImportStep.Mapping, table = reread, sourceVersion = 1, mappingInputVersion = 2))
        update(ImportDeckState(step = ImportStep.SubdeckSelect, table = reread, sourceVersion = 1, mappingInputVersion = 2))

        verticalOffset() shouldBe scrolled
    }

    @Test
    fun `연결 입력이 바뀌면 항목 연결 목록의 스크롤 위치를 처음으로 돌린다`() {
        val table =
            MappingStepTables.table(
                *List(8) { index -> MappingStepTables.vocab(id = "type$index", name = "유형 $index") }.toTypedArray(),
            )
        show(
            ImportDeckState(
                step = ImportStep.Mapping,
                table = table,
                mappings = MappingStepTables.defaultMappings(table),
                mappingInputVersion = 1,
            ),
        )
        scrollListUp()
        verticalOffset() shouldBeGreaterThan 0f

        update(
            ImportDeckState(
                step = ImportStep.Mapping,
                table = table,
                mappings = MappingStepTables.defaultMappings(table),
                mappingInputVersion = 2,
            ),
        )

        verticalOffset() shouldBe 0f
    }

    @Test
    fun `연결 입력이 바뀌어 다시 쓰이지 않는 단계의 저장 상태는 저장 인스턴스 상태에서 지운다`() {
        val registry = SaveableStateRegistry(restoredValues = null, canBeSaved = { true })
        val table =
            MappingStepTables.table(
                *List(8) { index -> MappingStepTables.vocab(id = "type$index", name = "유형 $index") }.toTypedArray(),
            )
        screenState =
            ImportDeckState(
                step = ImportStep.Mapping,
                table = table,
                mappings = MappingStepTables.defaultMappings(table),
                mappingInputVersion = 1,
            )
        composeRule.setContent {
            CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
                CaroTheme { ImportDeckScreen(state = screenState, onIntent = {}) }
            }
        }
        composeRule.waitForIdle()

        update(ImportDeckState(mappingInputVersion = 1))
        val savedWhileReusable = registry.performSave().toString()
        update(ImportDeckState(mappingInputVersion = 2))
        val savedAfterReplaced = registry.performSave().toString()

        listOf("Mapping-1" in savedWhileReusable, "Mapping-1" in savedAfterReplaced) shouldBe listOf(true, false)
    }

    private companion object {
        const val SHEET_COUNT = 30
        const val SUBDECK_COUNT = 30
        const val SHORT_HEIGHT = 480
    }
}
