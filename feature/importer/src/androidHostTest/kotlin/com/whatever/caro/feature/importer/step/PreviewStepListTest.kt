package com.whatever.caro.feature.importer.step

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.support.PreviewSamples
import com.whatever.caro.feature.importer.support.PreviewSamples.card
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** 미리보기의 필터 칩과 설명 한 줄, 카드 목록을 화면에서 확인한다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class PreviewStepListTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = PreviewScreenHost(composeRule)
    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    private val ankiState = PreviewSamples.state(PreviewSamples.ankiTable, PreviewSamples.ankiPreview)

    @Test
    fun `칩은 카드 수와 함께 전체, 빈 면, 글자 초과, 미디어 누락 순서로 한 줄에 놓인다`() {
        host.show(ankiState)

        val chips =
            listOf("전체" to "6", "빈 면" to "1", "글자 초과" to "2", "미디어 누락" to "2").map { (label, count) ->
                composeRule.onNode(isTab and hasText(label) and hasText(count)).bounds()
            }
        chips.zipWithNext().forEach { (left, right) ->
            (left.right <= right.left) shouldBe true
            left.top shouldBe right.top
        }
    }

    @Test
    fun `카드가 0장인 칩은 숨긴다`() {
        host.show(PreviewSamples.state(PreviewSamples.textTable, PreviewSamples.textPreview))

        composeRule.onAllNodes(isTab).fetchSemanticsNodes().size shouldBe 1
        composeRule.onNode(isTab and hasText("전체") and hasText("4")).assertIsSelected()
    }

    @Test
    fun `칩을 누르면 걸러진 목록과 설명 한 줄을 보이고 고른 칩만 선택 상태다`() {
        host.show(ankiState)

        composeRule.onNode(isTab and hasText("글자 초과")).performClick()

        composeRule.onNodeWithText("500자를 넘는 부분은 잘라서 가져와요.").assertIsDisplayed()
        composeRule.onNode(cardItem(PreviewSamples.ankiCards[2])).assertIsDisplayed()
        composeRule.onNode(cardItem(PreviewSamples.ankiCards[5])).assertIsDisplayed()
        composeRule.onNode(cardItem(PreviewSamples.ankiCards[0])).assertDoesNotExist()
        composeRule.onNode(isTab and hasText("글자 초과")).assertIsSelected()
        composeRule.onNode(isTab and hasText("전체")).assertIsNotSelected()
        host.intents shouldBe listOf(ImportDeckIntent.SelectPreviewFilter(ImportPreviewFilter.Truncated))
    }

    @Test
    fun `미디어 누락 설명은 칩의 카드 수가 아니라 지운 미디어 수를 쓴다`() {
        host.show(ankiState)

        composeRule.onNode(isTab and hasText("미디어 누락")).performClick()

        composeRule.onNodeWithText("미디어 파일 3개는 빼고 가져와요.").assertIsDisplayed()
    }

    @Test
    fun `빈 면 칩은 빈 면 문구를 넣는다고 설명하고 전체로 돌아가면 설명 줄이 사라진다`() {
        host.show(ankiState)

        composeRule.onNode(isTab and hasText("빈 면")).performClick()
        composeRule.onNodeWithText("빈 면에는 “(비어 있음)”을 넣어 가져와요.").assertIsDisplayed()
        composeRule.onNode(isTab and hasText("전체")).performClick()

        composeRule.onNodeWithText("빈 면에는", substring = true).assertDoesNotExist()
    }

    @Test
    fun `필터를 바꾸면 칩 이름과 결과 장수를 알리고 처음 그릴 때는 알리지 않는다`() {
        host.show(ankiState)
        composeRule.onNode(hasContentDescription("글자 초과 2장")).assertDoesNotExist()

        composeRule.onNode(isTab and hasText("글자 초과")).performClick()

        composeRule.onNode(hasContentDescription("글자 초과 2장")).assertExists()
    }

    @Test
    fun `필터 결과를 알리고 시간이 지나면 알림 문장을 비워 탐색에 남기지 않는다`() {
        host.show(ankiState)
        composeRule.onNode(isTab and hasText("글자 초과")).performClick()
        composeRule.onNode(hasContentDescription("글자 초과 2장")).assertExists()

        composeRule.mainClock.advanceTimeBy(ANNOUNCEMENT_SETTLE_MILLIS)

        composeRule.onNode(hasContentDescription("글자 초과 2장")).assertDoesNotExist()
    }

    @Test
    fun `필터 칩은 보이는 모양의 위아래 바깥까지 48dp 높이 안에서 눌린다`() {
        host.show(ankiState)

        tapChipTouchEdges("빈 면")

        host.intents shouldBe List(2) { ImportDeckIntent.SelectPreviewFilter(ImportPreviewFilter.EmptyReplaced) }
    }

    @Test
    fun `머리가 위에 붙어 칩 줄 아래로 카드가 지나가도 칩 둘레 48dp 높이 안의 누름은 칩이 받는다`() {
        val cards = List(40) { index -> card(index, "단어 $index", "뜻 $index") }
        host.show(PreviewSamples.state(PreviewSamples.textTable, PreviewSamples.preview(cards)))
        list().performScrollToIndex(20)

        tapChipTouchEdges("전체")

        host.intents shouldBe List(2) { ImportDeckIntent.SelectPreviewFilter(ImportPreviewFilter.All) }
    }

    @Test
    fun `글자 초과와 미디어 누락만 카드에 표시하고 앞뒤 모두 넘으면 두 개를 보인다`() {
        host.show(ankiState)

        val anyFlag = hasText("앞면 글자 초과") or hasText("뒷면 글자 초과") or hasText("미디어 누락")

        composeRule.onNode(cardItem(PreviewSamples.ankiCards[1])).assertIsDisplayed()
        composeRule.onNode(cardItem(PreviewSamples.ankiCards[1]) and anyFlag).assertDoesNotExist()
        composeRule.onNode(cardItem(PreviewSamples.ankiCards[2]) and hasText("뒷면 글자 초과")).assertExists()
        list().performScrollToNode(cardItem(PreviewSamples.ankiCards[5]))
        composeRule.onNode(cardItem(PreviewSamples.ankiCards[5]) and hasText("앞면 글자 초과") and hasText("뒷면 글자 초과")).assertExists()
        composeRule.onNode(cardItem(PreviewSamples.ankiCards[3]) and hasText("미디어 누락")).assertExists()
    }

    @Test
    fun `카드 유형이 2개 이상이면 앞면 줄 오른쪽 끝에 유형 이름을 둔다`() {
        host.show(ankiState)

        val front = composeRule.onNodeWithText("学校", useUnmergedTree = true).bounds()
        val type = composeRule.onAllNodesWithText(PreviewSamples.ANKI_TYPE, useUnmergedTree = true)[0].bounds()
        (type.left > front.left) shouldBe true
        (type.top < front.bottom) shouldBe true
        composeRule.onNode(cardItem(PreviewSamples.ankiCards[2]) and hasText(PreviewSamples.BASIC_TYPE)).assertExists()
    }

    @Test
    fun `카드 유형이 하나면 유형 이름을 두지 않는다`() {
        host.show(PreviewSamples.state(PreviewSamples.xlsxTable, PreviewSamples.xlsxPreview))

        composeRule.onNode(cardItem(PreviewSamples.xlsxCards[0])).assertIsDisplayed()
        composeRule.onNode(cardItem(PreviewSamples.xlsxCards[0]) and hasText("Day1")).assertDoesNotExist()
    }

    @Test
    fun `앞면은 두 줄, 뒷면은 세 줄까지 그리고 넘치면 말줄임한다`() {
        val long = card(0, "긴앞면".repeat(200), List(12) { line -> "뒷면 ${line + 1}번째 줄" }.joinToString("\n"))
        host.show(PreviewSamples.state(PreviewSamples.textTable, PreviewSamples.preview(listOf(long))))

        val front = composeRule.onNodeWithText(long.front, useUnmergedTree = true).textLayout()
        val back = composeRule.onNodeWithText(long.back, useUnmergedTree = true).textLayout()
        front.lineCount shouldBe 2
        front.hasVisualOverflow shouldBe true
        back.lineCount shouldBe 3
        back.hasVisualOverflow shouldBe true
    }

    @Test
    fun `스크롤해도 카드 목록 제목과 칩은 위에 붙어 남고 붙은 칩도 누를 수 있다`() {
        val cards = List(40) { index -> card(index, "단어 $index", "뜻 $index") }
        host.show(PreviewSamples.state(PreviewSamples.textTable, PreviewSamples.preview(cards)))

        list().performScrollToIndex(20)

        composeRule.onNodeWithText("카드 목록").assertIsDisplayed()
        composeRule.onNodeWithText("가져오기 설정").assertDoesNotExist()
        composeRule.onNode(isTab and hasText("전체")).assertIsDisplayed().performClick()
        host.intents shouldBe listOf(ImportDeckIntent.SelectPreviewFilter(ImportPreviewFilter.All))
    }

    @Test
    fun `칩이 화면 폭을 넘는 큰 글자에서는 칩 줄만 가로로 민다`() {
        host.show(ankiState, width = 360.dp, fontScale = 1.3f)

        val chipRow = composeRule.onNode(hasScrollAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange))
        (chipRow.horizontalScrollMax() > 0f) shouldBe true
        composeRule.onAllNodes(isTab).fetchSemanticsNodes().size shouldBe 4
    }

    private fun list() =
        composeRule.onNode(hasScrollAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))

    /** 보이는 칩의 위와 아래에서 48dp 높이가 끝나기 1dp 전 지점을 차례로 누른다. */
    private fun tapChipTouchEdges(label: String) {
        val chip = composeRule.onNode(isTab and hasText(label)).bounds()
        val reach = (MIN_TOUCH_HEIGHT - chip.height) / 2 - 1.dp
        (reach > 0.dp) shouldBe true
        val x = (chip.left + chip.right) / 2

        tapAt(x = x, y = chip.top - reach)
        tapAt(x = x, y = chip.bottom + reach)
    }

    private fun tapAt(
        x: Dp,
        y: Dp,
    ) {
        val position = with(composeRule.density) { Offset(x.toPx(), y.toPx()) }
        // 누름이 카드로 가면 전체 보기 창이 루트를 하나 더 만들므로 화면 루트를 집어 누른다.
        composeRule.onAllNodes(isRoot()).onFirst().performTouchInput { click(position) }
    }

    private companion object {
        const val ANNOUNCEMENT_SETTLE_MILLIS = 10_000L
        val MIN_TOUCH_HEIGHT = 48.dp
    }
}
