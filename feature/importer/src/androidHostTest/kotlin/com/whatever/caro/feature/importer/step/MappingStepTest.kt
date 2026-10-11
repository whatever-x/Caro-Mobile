package com.whatever.caro.feature.importer.step

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.component.isHeading
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.PersistentMap
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 문구는 Figma 프레임의 표시 문구다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class MappingStepTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host by lazy { MappingStepHost(composeRule) }

    private fun showVocab(
        mappings: PersistentMap<String, FieldMapping>? = null,
        applyEdits: Boolean = false,
        fontScale: Float = 1f,
        width: Dp = 393.dp,
    ) {
        val table = MappingStepTables.table(MappingStepTables.vocab())
        host.show(
            table = table,
            mappings = mappings ?: MappingStepTables.defaultMappings(table),
            applyEdits = applyEdits,
            fontScale = fontScale,
            width = width,
        )
    }

    private fun excludedVocab() =
        MappingStepTables.mappingsOf(
            MappingStepTables.mapping(MappingStepTables.VOCAB_ID, front = listOf("단어"), back = listOf("뜻", "예문", "읽기"), excluded = true),
        )

    private fun importToggle(): SemanticsNodeInteraction = composeRule.onNode(hasText(IMPORT_TOGGLE))

    private fun SemanticsNodeInteraction.toggleState(): ToggleableState = fetchSemanticsNode().config[SemanticsProperties.ToggleableState]

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    private fun top(node: SemanticsNodeInteraction): Float = node.fetchSemanticsNode().boundsInRoot.top

    private fun checkboxRightEdges(): List<Float> =
        composeRule.onAllNodes(isCheckboxRole).fetchSemanticsNodes().map { node -> node.boundsInRoot.right }

    @Test
    fun `안내 제목을 제목으로 읽히게 한다`() {
        showVocab()

        composeRule.onNode(hasText(HEADING) and isHeading).assertIsDisplayed()
    }

    @Test
    fun `고른 순서대로 줄을 바꿔 합친다는 안내를 보여준다`() {
        showVocab()

        composeRule.onNodeWithText(MERGE_HINT).assertIsDisplayed()
    }

    @Test
    fun `유형 카드는 유형 이름을 보여준다`() {
        showVocab()

        composeRule.onNodeWithText("일본어 단어").assertIsDisplayed()
    }

    @Test
    fun `유형 카드의 부제는 노트 수와 항목 수를 보여준다`() {
        showVocab()

        composeRule.onNodeWithText("노트 유형 · 1,204장 · 항목 4개").assertIsDisplayed()
    }

    @Test
    fun `고른 범위의 노트 수를 알면 부제의 장수로 쓴다`() {
        host.show(table = MappingStepTables.table(MappingStepTables.vocab(selectedNoteCount = 310)))

        composeRule.onNodeWithText("노트 유형 · 310장 · 항목 4개").assertIsDisplayed()
    }

    @Test
    fun `고른 범위에 노트가 없는 유형은 카드를 그리지 않는다`() {
        host.show(
            table =
                MappingStepTables.table(
                    MappingStepTables.vocab(),
                    MappingStepTables.vocab(id = "grammar", name = "문법", selectedNoteCount = 0),
                ),
        )

        composeRule.onNodeWithText("문법").assertDoesNotExist()
        composeRule.onAllNodes(hasText(IMPORT_TOGGLE)).assertCountEquals(1)
    }

    @Test
    fun `유형마다 카드 하나를 표 순서대로 위에서 아래로 그린다`() {
        host.show(
            table =
                MappingStepTables.table(
                    MappingStepTables.vocab(),
                    MappingStepTables.vocab(id = "grammar", name = "문법", noteCount = 310),
                ),
        )

        composeRule.onAllNodes(hasText(IMPORT_TOGGLE)).assertCountEquals(2)
        (top(composeRule.onNodeWithText("일본어 단어")) < top(composeRule.onNodeWithText("문법"))) shouldBe true
    }

    @Test
    fun `가져오기 줄은 가져오는 유형이면 켜진 상태다`() {
        showVocab()

        importToggle().toggleState() shouldBe ToggleableState.On
    }

    @Test
    fun `가져오기 줄은 끈 유형이면 꺼진 상태다`() {
        showVocab(mappings = excludedVocab())

        importToggle().toggleState() shouldBe ToggleableState.Off
    }

    @Test
    fun `가져오기 줄을 누르면 그 유형 id로 켜고 끄기를 요청한다`() {
        showVocab()

        importToggle().performClick()

        host.typeToggles shouldBe listOf(MappingStepTables.VOCAB_ID)
    }

    @Test
    fun `끈 유형의 가져오기 줄을 눌러도 같은 요청을 보낸다`() {
        showVocab(mappings = excludedVocab())

        importToggle().performClick()

        host.typeToggles shouldBe listOf(MappingStepTables.VOCAB_ID)
    }

    @Test
    fun `가져오기 줄은 체크박스 역할이다`() {
        showVocab()

        importToggle().assert(isCheckboxRole)
    }

    @Test
    fun `가져오기 줄의 높이는 48dp 이상이다`() {
        showVocab()

        importToggle().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `앞면과 뒷면 라벨을 보여준다`() {
        showVocab()

        composeRule.onNodeWithText("앞면에 넣을 항목").assertIsDisplayed()
        composeRule.onNodeWithText("뒷면에 넣을 항목").assertIsDisplayed()
    }

    @Test
    fun `앞면 라벨 아래에 앞면 칩을 놓고 그 아래에 뒷면 라벨과 뒷면 칩을 놓는다`() {
        showVocab()

        val frontLabel = top(composeRule.onNodeWithText("앞면에 넣을 항목"))
        val frontChip = top(composeRule.onNode(chosenChip("단어", "앞면", 1)))
        val backLabel = top(composeRule.onNodeWithText("뒷면에 넣을 항목"))
        val backChip = top(composeRule.onNode(chosenChip("뜻", "뒷면", 1)))

        (frontLabel < frontChip && frontChip < backLabel && backLabel < backChip) shouldBe true
    }

    @Test
    fun `기본 연결은 첫 항목을 앞면 1번째로 읽는다`() {
        showVocab()

        composeRule.onNode(chosenChip("단어", "앞면", 1)).assertIsDisplayed()
    }

    @Test
    fun `기본 연결은 나머지 항목을 뒷면에 고른 순서대로 읽는다`() {
        showVocab()

        composeRule.onNode(chosenChip("뜻", "뒷면", 1)).assertIsDisplayed()
        composeRule.onNode(chosenChip("예문", "뒷면", 2)).assertIsDisplayed()
        composeRule.onNode(chosenChip("읽기", "뒷면", 3)).assertIsDisplayed()
    }

    @Test
    fun `번호는 원본 항목 순서가 아니라 고른 순서를 따른다`() {
        showVocab(
            mappings =
                MappingStepTables.mappingsOf(
                    MappingStepTables.mapping(MappingStepTables.VOCAB_ID, front = listOf("읽기", "단어"), back = listOf("예문", "뜻")),
                ),
        )

        composeRule.onNode(chosenChip("읽기", "앞면", 1)).assertIsDisplayed()
        composeRule.onNode(chosenChip("단어", "앞면", 2)).assertIsDisplayed()
        composeRule.onNode(chosenChip("예문", "뒷면", 1)).assertIsDisplayed()
        composeRule.onNode(chosenChip("뜻", "뒷면", 2)).assertIsDisplayed()
    }

    @Test
    fun `다른 면에 놓인 항목의 칩은 이름만 읽고 순서 번호가 없다`() {
        showVocab()

        composeRule.onNode(plainChip("단어")).fetchSemanticsNode().readings() shouldBe listOf("단어")
    }

    @Test
    fun `체크 역할 줄은 가져오기 하나와 면마다 항목 수만큼의 칩 여덟 개다`() {
        showVocab()

        composeRule.onAllNodes(isCheckboxRole).assertCountEquals(9)
    }

    @Test
    fun `고른 칩을 누르면 그 면에서 그 항목의 선택 변경을 요청한다`() {
        showVocab()

        composeRule.onNode(chosenChip("뜻", "뒷면", 1)).performClick()

        host.fieldToggles shouldBe listOf(FieldToggle(MappingStepTables.VOCAB_ID, FieldMapping.BACK, "뜻"))
    }

    @Test
    fun `앞면의 고른 칩을 누르면 앞면 기준으로 요청한다`() {
        showVocab()

        composeRule.onNode(chosenChip("단어", "앞면", 1)).performClick()

        host.fieldToggles shouldBe listOf(FieldToggle(MappingStepTables.VOCAB_ID, FieldMapping.FRONT, "단어"))
    }

    @Test
    fun `다른 면에 놓인 항목의 칩을 누르면 눌린 칩의 면으로 옮겨 달라고 요청한다`() {
        showVocab()

        composeRule.onNode(plainChip("뜻")).performClick()

        host.fieldToggles shouldBe listOf(FieldToggle(MappingStepTables.VOCAB_ID, FieldMapping.FRONT, "뜻"))
    }

    @Test
    fun `어느 면에도 없는 항목은 면마다 칩이 있고 위쪽 칩은 앞면 아래쪽 칩은 뒷면 기준으로 요청한다`() {
        showVocab(
            mappings =
                MappingStepTables.mappingsOf(
                    MappingStepTables.mapping(MappingStepTables.VOCAB_ID, front = listOf("단어"), back = listOf("뜻")),
                ),
        )

        composeRule.onAllNodes(plainChip("예문")).assertCountEquals(2)
        composeRule.onAllNodes(plainChip("예문"))[0].performClick()
        composeRule.onAllNodes(plainChip("예문"))[1].performClick()

        host.fieldToggles shouldBe
            listOf(
                FieldToggle(MappingStepTables.VOCAB_ID, FieldMapping.FRONT, "예문"),
                FieldToggle(MappingStepTables.VOCAB_ID, FieldMapping.BACK, "예문"),
            )
    }

    @Test
    fun `칩을 눌러 항목을 옮기면 번호가 새로 매겨져 다시 그려진다`() {
        showVocab(applyEdits = true)

        composeRule.onNode(plainChip("뜻")).performClick()

        composeRule.onNode(chosenChip("뜻", "앞면", 2)).assertIsDisplayed()
        composeRule.onNode(chosenChip("예문", "뒷면", 1)).assertIsDisplayed()
    }

    @Test
    fun `항목이 열두 개여도 칩은 가로 스크롤 없이 줄을 바꿔 화면 안에 모두 보여준다`() {
        host.show(table = MappingStepTables.table(MappingStepTables.wide(fieldCount = 12)))

        composeRule.onAllNodes(hasHorizontalScrollRange).assertCountEquals(0)
        composeRule.onAllNodes(isCheckboxRole).assertCountEquals(25)
        checkboxRightEdges().all { right -> right <= host.contentWidthPx } shouldBe true
    }

    @Test
    fun `목록은 세로 스크롤 컨테이너 하나뿐이다`() {
        showVocab()

        composeRule.onAllNodes(hasVerticalScrollRange).assertCountEquals(1)
    }

    @Test
    fun `80자를 넘는 항목 이름은 그리기 전에 80자로 줄여 말줄임을 붙인다`() {
        host.show(table = MappingStepTables.table(MappingStepTables.vocab(fieldNames = listOf("단어", LONG_NAME))))

        composeRule.onAllNodes(readsContaining(LONG_NAME)).assertCountEquals(0)
        composeRule.onAllNodes(readsContaining(LONG_NAME_SHOWN)).assertCountEquals(2)
    }

    @Test
    fun `긴 항목 이름 칩은 2줄까지 보이고 마지막 줄이 말줄임으로 끝난다`() {
        host.show(table = MappingStepTables.table(MappingStepTables.vocab(fieldNames = listOf("단어", LONG_NAME))))

        val layout = composeRule.onAllNodes(hasText(LONG_NAME_SHOWN), useUnmergedTree = true)[0].textLayout()

        listOf(layout.lineCount, layout.isLineEllipsized(1)) shouldBe listOf(2, true)
    }

    @Test
    fun `글자를 200퍼센트로 키우면 칩이 고정 높이에 갇히지 않고 40dp 이상으로 커진다`() {
        showVocab(fontScale = 2f)

        composeRule.onNode(chosenChip("뜻", "뒷면", 1)).assertHeightIsAtLeast(40.dp)
    }

    @Test
    fun `글자를 200퍼센트로 키워도 칩은 좁은 화면 너비를 넘지 않는다`() {
        showVocab(fontScale = 2f, width = 360.dp)

        composeRule.onAllNodes(isCheckboxRole).assertCountEquals(9)
        checkboxRightEdges().all { right -> right <= host.contentWidthPx } shouldBe true
    }

    @Test
    fun `끈 유형의 칩은 모두 사용할 수 없음으로 읽힌다`() {
        showVocab(mappings = excludedVocab())

        MappingStepTables.VOCAB_FIELDS.forEach { name ->
            composeRule.onAllNodes(unavailableChip(name)).assertCountEquals(2)
        }
    }

    @Test
    fun `켠 유형의 칩은 사용할 수 없음으로 읽히지 않는다`() {
        showVocab()

        composeRule.onNode(chosenChip("뜻", "뒷면", 1)).assertIsDisplayed()
        MappingStepTables.VOCAB_FIELDS.forEach { name ->
            composeRule.onAllNodes(unavailableChip(name)).assertCountEquals(0)
        }
    }

    @Test
    fun `끈 유형의 칩은 누를 수 없다`() {
        showVocab(mappings = excludedVocab())

        composeRule.onAllNodes(unavailableChip("뜻"))[0].assertIsNotEnabled()
        composeRule.onAllNodes(unavailableChip("뜻"))[0].performClick()

        host.fieldToggles shouldBe emptyList()
    }

    @Test
    fun `가져오기를 끄면 그 유형의 칩이 사용할 수 없음으로 바뀐다`() {
        showVocab(applyEdits = true)

        importToggle().performClick()

        composeRule.onAllNodes(unavailableChip("뜻")).assertCountEquals(2)
    }

    @Test
    fun `가져오기를 껐다 다시 켜면 칩이 다시 눌린다`() {
        showVocab(mappings = excludedVocab(), applyEdits = true)

        importToggle().performClick()
        composeRule.onNode(chosenChip("뜻", "뒷면", 1)).performClick()

        host.fieldToggles shouldBe listOf(FieldToggle(MappingStepTables.VOCAB_ID, FieldMapping.BACK, "뜻"))
    }

    private companion object {
        const val HEADING = "앞뒷면에 넣을 항목을 골라 주세요"
        const val MERGE_HINT = "한 면에 여러 항목을 고르면 고른 순서대로 줄을 바꿔 합쳐요."
        const val IMPORT_TOGGLE = "가져오기"
        val LONG_NAME = "긴".repeat(100)
        val LONG_NAME_SHOWN = "긴".repeat(79) + "…"
    }
}
