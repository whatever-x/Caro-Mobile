package com.whatever.caro.feature.importer.step

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 표 크기는 Figma의 2열 이하, 큰 글자, 20열 넘는 결과 프레임 값을 따른다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class TextCheckStepTableTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host by lazy { TextCheckStepHost(composeRule) }

    private val hasHorizontalScroll = SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)
    private val hasVerticalScroll = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

    private fun unmerged(text: String): SemanticsNodeInteraction = composeRule.onNode(hasText(text), useUnmergedTree = true)

    private fun unmergedStartsWith(prefix: String): SemanticsNodeInteraction =
        composeRule.onNode(hasText(prefix, substring = true), useUnmergedTree = true)

    private fun left(text: String): Float = unmerged(text).fetchSemanticsNode().boundsInRoot.left

    private fun horizontalOffset(): Float =
        composeRule
            .onNode(hasHorizontalScroll)
            .fetchSemanticsNode()
            .config[SemanticsProperties.HorizontalScrollAxisRange]
            .value()

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    // 목록이든 일반 스크롤이든 같은 방식으로 대상까지 내린다.
    private fun scrollToVisible(matcher: SemanticsMatcher) {
        val container = composeRule.onNode(hasVerticalScroll)
        if (SemanticsActions.ScrollToIndex in container.fetchSemanticsNode().config) {
            container.performScrollToNode(matcher)
        } else {
            composeRule.onNode(matcher).performScrollTo()
        }
    }

    private fun longCellTable() =
        TextCheckStepTables.text(
            fieldNames = TextCheckStepTables.GENERATED_NAMES.take(3),
            rows = listOf(listOf(LONG_VALUE, "b", "c")),
        )

    @Test
    fun `데이터 처음 4줄의 값을 보여준다`() {
        host.show()

        listOf("学校", "先生", "読む", "毎日").forEach { value -> unmerged(value).assertExists() }
    }

    @Test
    fun `5번째 줄부터는 그리지 않는다`() {
        host.show()

        unmerged("友達").assertDoesNotExist()
    }

    @Test
    fun `열 이름이 있으면 머리 줄에 그 이름을 보여준다`() {
        host.show(table = TextCheckStepTables.text(fieldNames = TextCheckStepTables.HEADER_NAMES))

        TextCheckStepTables.HEADER_NAMES.forEach { name -> unmerged(name).assertExists() }
    }

    @Test
    fun `파서가 만든 열 이름은 열 N으로 보여준다`() {
        host.show()

        unmerged("열 3").assertExists()
    }

    @Test
    fun `스크린 리더는 열 이름 없이 셀을 따로 읽지 않고 줄 하나를 한 번에 읽는다`() {
        host.show()

        composeRule.onAllNodes(readsAs(FIRST_ROW_READING)).assertCountEquals(1)
    }

    @Test
    fun `줄을 한 번에 읽을 때 열 이름이 있으면 그 이름을 쓴다`() {
        host.show(table = TextCheckStepTables.text(fieldNames = TextCheckStepTables.HEADER_NAMES))

        composeRule.onAllNodes(readsAs("1행, 단어 学校, 뜻 학교, 읽기 がっこう, 예문 学校に行きます。")).assertCountEquals(1)
    }

    @Test
    fun `네 번째 줄도 줄 번호와 함께 한 번에 읽는다`() {
        host.show()

        composeRule.onAllNodes(readsAs(FOURTH_ROW_READING)).assertCountEquals(1)
    }

    @Test
    fun `줄마다 읽는 노드가 따로 있어 한 줄을 읽고 다음 줄로 넘어갈 수 있다`() {
        host.show()

        val first = composeRule.onNode(readsAs(FIRST_ROW_READING)).fetchSemanticsNode().id
        val fourth = composeRule.onNode(readsAs(FOURTH_ROW_READING)).fetchSemanticsNode().id

        (first != fourth) shouldBe true
    }

    @Test
    fun `2열 이하 결과는 가로 스크롤 없이 폭을 채운다`() {
        host.show(table = TextCheckStepTables.text(fieldNames = listOf("열 1", "열 2"), rowCount = 120))

        composeRule.onAllNodes(hasHorizontalScroll).assertCountEquals(0)
    }

    @Test
    fun `2열 결과의 두 번째 열은 폭을 나눠 가져 화면 가운데 근처에서 시작한다`() {
        host.show(table = TextCheckStepTables.text(fieldNames = listOf("열 1", "열 2"), rowCount = 120))

        val secondColumnLeft = with(composeRule.density) { left("열 2").toDp() }

        secondColumnLeft.value shouldBeGreaterThanOrEqual 150f
    }

    @Test
    fun `3열 이상 결과는 표만 가로로 스크롤할 수 있다`() {
        host.show()

        composeRule.onAllNodes(hasHorizontalScroll).assertCountEquals(1)
        composeRule
            .onNode(
                hasHorizontalScroll,
            ).fetchSemanticsNode()
            .config[SemanticsProperties.HorizontalScrollAxisRange]
            .maxValue() shouldBeGreaterThan
            0f
    }

    @Test
    fun `3열 이상 결과는 세 번째 열이 오른쪽 끝에 약 40dp 보인다`() {
        host.show(table = TextCheckStepTables.text(fieldNames = TextCheckStepTables.HEADER_NAMES))

        val tableRight =
            composeRule
                .onNode(hasHorizontalScroll)
                .fetchSemanticsNode()
                .boundsInRoot.right
        val visiblePx = tableRight - left("읽기")

        with(composeRule.density) { visiblePx.toDp().value } shouldBe (42f plusOrMinus 10f)
    }

    @Test
    fun `가로로 스크롤하면 머리 줄도 본문 줄과 함께 움직인다`() {
        host.show(table = TextCheckStepTables.text(fieldNames = TextCheckStepTables.HEADER_NAMES))
        val headerBefore = left("단어")

        composeRule.onNode(hasHorizontalScroll).performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        left("단어") shouldBeLessThan headerBefore
        left("단어") shouldBe (left("学校") plusOrMinus 1f)
    }

    @Test
    fun `20열 넘는 결과는 20번째 열까지만 보여준다`() {
        host.show(table = TextCheckStepTables.wide(columnCount = 25))

        unmerged("열 20").assertExists()
        unmerged("열 21").assertDoesNotExist()
    }

    @Test
    fun `표 셀은 2줄까지 보이고 마지막 줄이 말줄임으로 끝난다`() {
        host.show(table = longCellTable())

        val layout = unmergedStartsWith(LONG_PREFIX).textLayout()

        listOf(layout.lineCount, layout.isLineEllipsized(1)) shouldBe listOf(2, true)
    }

    @Test
    fun `표 셀은 Text에 넘기기 전에 200자로 줄여 말줄임을 붙인다`() {
        host.show(table = longCellTable())

        val shown =
            unmergedStartsWith(LONG_PREFIX)
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .single()
                .text

        shown.length shouldBeLessThanOrEqual 200
        shown shouldEndWith "…"
    }

    @Test
    fun `세로 스크롤 컨테이너는 하나뿐이다`() {
        host.show()

        composeRule.onAllNodes(hasVerticalScroll).assertCountEquals(1)
    }

    @Test
    fun `높이가 모자라면 스크롤로 20열 안내까지 닿고 다음 버튼은 계속 보인다`() {
        host.show(table = TextCheckStepTables.wide(columnCount = 25), height = 420.dp)

        scrollToVisible(hasText(MORE_COLUMNS_HINT))

        composeRule.onNodeWithText(MORE_COLUMNS_HINT).assertIsDisplayed()
        composeRule.onNodeWithText(NEXT).assertIsDisplayed()
    }

    @Test
    fun `글자 130퍼센트에서는 표 대신 줄마다 열 이름과 값을 쌓은 카드를 그린다`() {
        host.show(fontScale = 1.3f)

        listOf("열 1: 学校", "열 2: 학교", "열 3: がっこう", "열 4: 学校に行きます。").forEach { line -> unmerged(line).assertExists() }
    }

    @Test
    fun `글자 130퍼센트에서는 표의 머리 줄이 없다`() {
        host.show(fontScale = 1.3f)

        unmerged("열 1").assertDoesNotExist()
    }

    @Test
    fun `글자 130퍼센트 카드는 줄 번호를 보여준다`() {
        host.show(fontScale = 1.3f)

        unmerged("1행").assertExists()
    }

    @Test
    fun `글자 130퍼센트 카드도 처음 4줄만 그린다`() {
        host.show(fontScale = 1.3f)

        unmerged("4행").assertExists()
        unmerged("5행").assertDoesNotExist()
    }

    @Test
    fun `글자 130퍼센트 카드는 가로 스크롤 없이 세로로만 스크롤한다`() {
        host.show(fontScale = 1.3f)

        composeRule.onAllNodes(hasHorizontalScroll).assertCountEquals(0)
        composeRule.onAllNodes(hasScrollAction()).assertCountEquals(1)
    }

    @Test
    fun `글자 130퍼센트 카드 하나도 스크린 리더가 한 번에 읽는다`() {
        host.show(fontScale = 1.3f)

        composeRule.onAllNodes(readsAs(FIRST_ROW_READING)).assertCountEquals(1)
    }

    @Test
    fun `글자 130퍼센트 카드의 긴 값은 2줄까지 보이고 마지막 줄이 말줄임으로 끝난다`() {
        host.show(table = longCellTable(), fontScale = 1.3f)

        val layout = unmergedStartsWith("열 1: $LONG_PREFIX").textLayout()

        listOf(layout.lineCount, layout.isLineEllipsized(1)) shouldBe listOf(2, true)
    }

    @Test
    fun `글자 130퍼센트에서도 표 정보 줄은 잘리지 않고 다 보인다`() {
        host.show(fontScale = 1.3f)

        val info = composeRule.onNodeWithText("열 4개 · 10줄 중 처음 4줄")

        info.assertIsDisplayed()
        info.textLayout().hasVisualOverflow shouldBe false
    }

    @Test
    fun `글자 130퍼센트 미만에서는 줄 카드가 아니라 표를 그린다`() {
        host.show(fontScale = 1.15f)

        unmerged("열 1: 学校").assertDoesNotExist()
        composeRule.onAllNodes(hasHorizontalScroll).assertCountEquals(1)
    }

    @Test
    fun `최대 글자 크기에서도 구분자 칩이 모두 화면 안에 보인다`() {
        host.show(delimiter = '|', width = 360.dp, height = 640.dp, fontScale = 2f)

        listOf("탭", "쉼표", "세미콜론", "세로선 |").forEach { label -> composeRule.onNodeWithText(label).assertIsDisplayed() }
    }

    @Test
    fun `최대 글자 크기에서도 스크롤로 마지막 카드까지 닿고 다음 버튼은 계속 보인다`() {
        host.show(width = 360.dp, height = 640.dp, fontScale = 2f)

        scrollToVisible(readsAs(FOURTH_ROW_READING))

        composeRule.onNode(readsAs(FOURTH_ROW_READING)).assertIsDisplayed()
        composeRule.onNodeWithText(NEXT).assertIsDisplayed()
    }

    private companion object {
        const val NEXT = "다음"
        const val MORE_COLUMNS_HINT = "나머지 열은 항목 연결에서 고를 수 있어요."
        const val LONG_PREFIX = "가나다라마바사"
        const val FIRST_ROW_READING = "1행, 열 1 学校, 열 2 학교, 열 3 がっこう, 열 4 学校に行きます。"
        const val FOURTH_ROW_READING = "4행, 열 1 毎日, 열 2 매일, 열 3 まいにち, 열 4 毎日勉強します。"
        val LONG_VALUE = "가나다라마바사아자차카타파하".repeat(20)
    }
}
