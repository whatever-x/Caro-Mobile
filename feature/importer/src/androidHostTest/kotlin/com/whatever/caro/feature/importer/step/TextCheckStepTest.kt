package com.whatever.caro.feature.importer.step

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
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
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.whatever.caro.feature.importer.component.isHeading
import io.kotest.matchers.shouldBe
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
class TextCheckStepTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host by lazy { TextCheckStepHost(composeRule) }

    private val isRadio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
    private val isPoliteLiveRegion = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
    private val isToggleable = SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState)

    private fun chip(label: String): SemanticsNodeInteraction = composeRule.onNode(hasText(label) and isRadio)

    private fun chipLabelsLeftToRight(): List<String> =
        composeRule
            .onAllNodes(isRadio)
            .fetchSemanticsNodes()
            .sortedWith(compareBy({ node -> node.boundsInRoot.top }, { node -> node.boundsInRoot.left }))
            .map { node -> node.config[SemanticsProperties.Text].joinToString(separator = "") { text -> text.text } }

    private fun headerRow(): SemanticsNodeInteraction = composeRule.onNode(hasText(HEADER_ROW))

    private fun nextButton(): SemanticsNodeInteraction = composeRule.onNodeWithText(NEXT)

    private fun SemanticsNodeInteraction.toggleState(): ToggleableState = fetchSemanticsNode().config[SemanticsProperties.ToggleableState]

    private fun showReparsing() {
        host.show(table = TextCheckStepTables.text(fieldNames = TextCheckStepTables.HEADER_NAMES), isReparsing = true, manualClock = true)
    }

    @Test
    fun `안내 제목을 제목으로 읽히게 한다`() {
        host.show()

        composeRule.onNode(hasText(HEADING) and isHeading).assertIsDisplayed()
    }

    @Test
    fun `자동으로 고른 설정이라는 안내를 보여준다`() {
        host.show()

        composeRule.onNodeWithText(AUTO_HINT).assertIsDisplayed()
    }

    @Test
    fun `구분자 라벨을 보여준다`() {
        host.show()

        composeRule.onNodeWithText("구분자").assertIsDisplayed()
    }

    @Test
    fun `나뉜 결과 라벨을 보여준다`() {
        host.show()

        composeRule.onNodeWithText("나뉜 결과").assertIsDisplayed()
    }

    @Test
    fun `기본 구분자 칩은 탭 쉼표 세미콜론 순서로 셋뿐이다`() {
        host.show(delimiter = '\t')

        chipLabelsLeftToRight() shouldBe listOf("탭", "쉼표", "세미콜론")
    }

    @Test
    fun `탭을 쓰고 있으면 탭 칩만 선택 상태다`() {
        host.show(delimiter = '\t')

        chip("탭").assertIsSelected()
        chip("쉼표").assertIsNotSelected()
        chip("세미콜론").assertIsNotSelected()
    }

    @Test
    fun `쉼표를 쓰고 있으면 쉼표 칩만 선택 상태다`() {
        host.show(delimiter = ',')

        chip("탭").assertIsNotSelected()
        chip("쉼표").assertIsSelected()
        chip("세미콜론").assertIsNotSelected()
    }

    @Test
    fun `세로선을 쓰고 있으면 세로선 칩이 하나 더 생기고 선택 상태다`() {
        host.show(delimiter = '|')

        chipLabelsLeftToRight() shouldBe listOf("탭", "쉼표", "세미콜론", "세로선 |")
        chip("세로선 |").assertIsSelected()
    }

    @Test
    fun `세로선을 쓰고 있으면 기본 세 칩은 선택 상태가 아니다`() {
        host.show(delimiter = '|')

        chip("탭").assertIsNotSelected()
        chip("쉼표").assertIsNotSelected()
        chip("세미콜론").assertIsNotSelected()
    }

    @Test
    fun `공백을 쓰고 있으면 공백 칩이 하나 더 생기고 선택 상태다`() {
        host.show(delimiter = ' ')

        chip("공백").assertIsSelected()
    }

    @Test
    fun `콜론을 쓰고 있으면 콜론 칩이 하나 더 생기고 선택 상태다`() {
        host.show(delimiter = ':')

        chip("콜론 :").assertIsSelected()
    }

    @Test
    fun `이름이 없는 구분자는 그 문자를 칩으로 하나 더 보여주고 선택 상태로 둔다`() {
        host.show(delimiter = '#')

        chip("#").assertIsSelected()
    }

    @Test
    fun `구분자 칩은 하나만 고르는 그룹의 라디오 버튼 역할이다`() {
        host.show(delimiter = '|')

        composeRule.onAllNodes(isRadio).assertCountEquals(4)
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)).assertCountEquals(1)
    }

    @Test
    fun `쉼표 칩을 누르면 쉼표로 선택을 요청한다`() {
        host.show(delimiter = '\t')

        chip("쉼표").performClick()

        host.delimiters shouldBe listOf(',')
    }

    @Test
    fun `세미콜론 칩을 누르면 세미콜론으로 선택을 요청한다`() {
        host.show(delimiter = '\t')

        chip("세미콜론").performClick()

        host.delimiters shouldBe listOf(';')
    }

    @Test
    fun `탭 칩을 누르면 탭으로 선택을 요청한다`() {
        host.show(delimiter = ',')

        chip("탭").performClick()

        host.delimiters shouldBe listOf('\t')
    }

    @Test
    fun `세로선 칩이 선택된 상태에서 쉼표 칩을 누르면 쉼표로 선택을 요청한다`() {
        host.show(delimiter = '|')

        chip("쉼표").performClick()

        host.delimiters shouldBe listOf(',')
    }

    @Test
    fun `첫 줄은 제목이 켜져 있으면 줄이 켜진 상태다`() {
        host.show(hasHeaderRow = true)

        headerRow().toggleState() shouldBe ToggleableState.On
    }

    @Test
    fun `첫 줄은 제목이 꺼져 있으면 줄이 꺼진 상태다`() {
        host.show(hasHeaderRow = false)

        headerRow().toggleState() shouldBe ToggleableState.Off
    }

    @Test
    fun `첫 줄은 제목 줄을 눌러 켜진 값을 끈다`() {
        host.show(hasHeaderRow = true)

        headerRow().performClick()

        host.headerChanges shouldBe listOf(false)
    }

    @Test
    fun `첫 줄은 제목 줄을 눌러 꺼진 값을 켠다`() {
        host.show(hasHeaderRow = false)

        headerRow().performClick()

        host.headerChanges shouldBe listOf(true)
    }

    @Test
    fun `첫 줄은 제목 줄은 체크박스 역할의 toggleable이고 높이가 48dp 이상이다`() {
        host.show()

        headerRow().assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
        headerRow().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `체크 줄의 toggleable은 첫 줄은 제목 줄 하나뿐이다`() {
        host.show()

        composeRule.onAllNodes(isToggleable, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `머리말에 항목 이름이 있으면 첫 줄은 제목 체크박스를 숨긴다`() {
        host.show(table = TextCheckStepTables.text(fieldNames = TextCheckStepTables.HEADER_NAMES, columnsFromHeader = true))

        composeRule.onNodeWithText(HEADER_ROW).assertDoesNotExist()
        composeRule.onAllNodes(isToggleable, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun `머리말에 항목 이름이 있으면 그 이름을 쓴다는 문구를 대신 보여준다`() {
        host.show(table = TextCheckStepTables.text(fieldNames = TextCheckStepTables.HEADER_NAMES, columnsFromHeader = true))

        composeRule.onNodeWithText(HEADER_FROM_FILE).assertIsDisplayed()
    }

    @Test
    fun `머리말에 항목 이름이 없으면 그 이름을 쓴다는 문구가 없다`() {
        host.show()

        composeRule.onNodeWithText(HEADER_FROM_FILE).assertDoesNotExist()
    }

    @Test
    fun `표 정보는 열 수와 전체 줄 수 중 보이는 줄 수를 보여준다`() {
        host.show(table = TextCheckStepTables.text())

        composeRule.onNodeWithText("열 4개 · 10줄 중 처음 4줄").assertIsDisplayed()
    }

    @Test
    fun `2열 이하 결과의 표 정보는 열 2개와 전체 줄 수를 보여준다`() {
        host.show(table = TextCheckStepTables.text(fieldNames = listOf("열 1", "열 2"), rowCount = 120))

        composeRule.onNodeWithText("열 2개 · 120줄 중 처음 4줄").assertIsDisplayed()
    }

    @Test
    fun `표 정보의 줄 수는 천 단위 쉼표를 붙인다`() {
        host.show(table = TextCheckStepTables.text(fieldNames = listOf("열 1", "열 2"), rowCount = 1_204))

        composeRule.onNodeWithText("열 2개 · 1,204줄 중 처음 4줄").assertIsDisplayed()
    }

    @Test
    fun `20열을 넘으면 표 정보에 전체 열 수 중 보이는 열 수를 함께 보여준다`() {
        host.show(table = TextCheckStepTables.wide(columnCount = 25))

        composeRule.onNodeWithText("열 25개 중 20개 · 10줄 중 처음 4줄").assertIsDisplayed()
    }

    @Test
    fun `20열을 넘으면 나머지 열은 항목 연결에서 고를 수 있다는 안내를 보여준다`() {
        host.show(table = TextCheckStepTables.wide(columnCount = 25))

        composeRule.onNodeWithText(MORE_COLUMNS_HINT).assertExists()
    }

    @Test
    fun `20열 이하면 나머지 열 안내가 없다`() {
        host.show(table = TextCheckStepTables.wide(columnCount = 20))

        composeRule.onNodeWithText(MORE_COLUMNS_HINT).assertDoesNotExist()
    }

    @Test
    fun `표 정보는 polite live region이라 다시 나눈 결과가 나오면 스크린 리더로 알린다`() {
        host.show()

        composeRule.onNodeWithText("열 4개 · 10줄 중 처음 4줄").assert(isPoliteLiveRegion)
    }

    @Test
    fun `다음 버튼은 켜져 있고 누르면 다음을 한 번 요청한다`() {
        host.show()

        nextButton().assertIsEnabled()
        nextButton().performClick()

        host.nextClicks shouldBe 1
    }

    @Test
    fun `다시 나누는 중이 아니면 로딩 문구가 없다`() {
        host.show()

        composeRule.onNodeWithText(RESPLIT).assertDoesNotExist()
    }

    @Test
    fun `다시 나누기가 200ms 안에 끝날 수 있으므로 그 안에는 로딩 문구를 보여주지 않는다`() {
        showReparsing()

        composeRule.mainClock.advanceTimeBy(100L)

        composeRule.onNodeWithText(RESPLIT).assertDoesNotExist()
    }

    @Test
    fun `다시 나누는 중이 200ms를 넘으면 결과 표 자리에 다시 나누고 있어요를 보여준다`() {
        showReparsing()

        composeRule.mainClock.advanceTimeBy(CARD_DELAY_MILLIS)

        composeRule.onNodeWithText(RESPLIT).assertIsDisplayed()
    }

    @Test
    fun `다시 나누고 있어요 문구는 polite live region이다`() {
        showReparsing()

        composeRule.mainClock.advanceTimeBy(CARD_DELAY_MILLIS)

        composeRule.onNodeWithText(RESPLIT).assert(isPoliteLiveRegion)
    }

    @Test
    fun `다시 나누는 중에는 이전 줄의 값이 접근성 트리에 없다`() {
        showReparsing()

        composeRule.mainClock.advanceTimeBy(CARD_DELAY_MILLIS)

        composeRule.onNodeWithText("学校").assertDoesNotExist()
        composeRule.onNodeWithText("학교").assertDoesNotExist()
    }

    @Test
    fun `다시 나누는 중에는 이전 표의 열 이름도 접근성 트리에 없다`() {
        showReparsing()

        composeRule.mainClock.advanceTimeBy(CARD_DELAY_MILLIS)

        composeRule.onNodeWithText("단어").assertDoesNotExist()
    }

    @Test
    fun `다시 나누는 중에도 표의 틀로 열 이름 줄은 그대로 그린다`() {
        showReparsing()

        composeRule.mainClock.advanceTimeBy(CARD_DELAY_MILLIS)

        composeRule.onNodeWithText("단어", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `다시 나누는 중에는 다음 버튼이 꺼진다`() {
        showReparsing()

        nextButton().assertIsNotEnabled()
    }

    @Test
    fun `다시 나누는 중에도 구분자 칩을 누르면 선택을 요청한다`() {
        showReparsing()

        chip("쉼표").performClick()

        host.delimiters shouldBe listOf(',')
    }

    @Test
    fun `다시 나누는 중에도 안내 제목은 가려지지 않는다`() {
        showReparsing()

        composeRule.mainClock.advanceTimeBy(CARD_DELAY_MILLIS)

        composeRule.onNode(hasText(HEADING) and isHeading).assertIsDisplayed()
    }

    private companion object {
        const val HEADING = "텍스트가 어떻게 나뉘었는지 확인해 주세요"
        const val AUTO_HINT = "자동으로 고른 설정이에요. 다르게 나뉘었다면 바꿔 주세요."
        const val HEADER_ROW = "첫 줄은 제목"
        const val HEADER_FROM_FILE = "머리말의 항목 이름을 써요"
        const val MORE_COLUMNS_HINT = "나머지 열은 항목 연결에서 고를 수 있어요."
        const val RESPLIT = "다시 나누고 있어요"
        const val NEXT = "다음"
        const val CARD_DELAY_MILLIS = 250L
    }
}
