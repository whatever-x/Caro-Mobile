package com.whatever.caro.feature.importer.step

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.whatever.caro.core.importparser.model.ParsedLayout
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.SettingsSamples
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class TextCheckStepTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = SettingsScreenHost(composeRule)
    private val isRadio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
    private val hasHorizontalScroll = SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)

    private fun chip(label: String) = composeRule.onNode(hasText(label) and isRadio)

    @Test
    fun `자동으로 고른 구분 기준 칩이 선택되어 있고 다른 칩을 누르면 구분 기준 입력을 보낸다`() {
        host.show(SettingsSamples.delimiterState())

        chip("탭 키 간격").assertIsSelected()
        chip("쉼표").assertIsNotSelected().performClick()

        host.intents shouldBe listOf(ImportDeckIntent.SelectDelimiter(','))
    }

    @Test
    fun `구분 기준 칩은 보이는 모양의 위아래 바깥까지 48dp 높이 안에서 눌린다`() {
        host.show(SettingsSamples.delimiterState())

        host.tapTouchEdges(chip("쉼표").bounds())

        host.intents shouldBe List(2) { ImportDeckIntent.SelectDelimiter(',') }
    }

    @Test
    fun `3열 이상 표는 세 번째 열을 오른쪽 끝에 걸쳐 보이고 가로로 밀면 머리 줄이 본문 줄과 함께 움직인다`() {
        host.show(SettingsSamples.delimiterState(ImportTestTables.text(rows = 4, columns = COLUMN_COUNT)))
        val scroll = composeRule.onNode(hasHorizontalScroll, useUnmergedTree = true)
        val header = scroll.onChildAt(0)
        val firstRow = composeRule.onNode(hasContentDescription("1번째 줄", substring = true))
        val row = firstRow.bounds()
        val columnWidth = (row.width - TABLE_START_PADDING) / COLUMN_COUNT

        (SemanticsProperties.ContentDescription in header.fetchSemanticsNode().config) shouldBe false
        (scroll.bounds().right - (row.left + TABLE_START_PADDING + columnWidth * 2)).value shouldBe (THIRD_COLUMN_PEEK plusOrMinus 1f)

        val headerBefore = header.bounds().left
        scroll.performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        (header.bounds().left < headerBefore) shouldBe true
        header.bounds().left.value shouldBe (firstRow.bounds().left.value plusOrMinus 0.5f)
    }

    @Test
    fun `첫 카드 미리보기와 나뉜 결과 표를 보인다`() {
        host.show(SettingsSamples.delimiterState())

        composeRule.onNode(hasText("사과") and hasText("apple")).assertIsDisplayed()
        composeRule.onNodeWithText("열 2개, 45줄 중 처음 4줄").assertIsDisplayed()
    }

    @Test
    fun `첫 줄 체크를 누르면 첫 줄 입력을 보내고 꺼진 설명을 함께 읽는다`() {
        host.show(SettingsSamples.delimiterState())

        composeRule.onNode(hasText("첫 줄을 열 이름으로 쓰기") and hasText("첫 줄도 카드로 가져와요.")).performClick()

        host.intents shouldBe listOf(ImportDeckIntent.ChangeHeaderRow(true))
    }

    @Test
    fun `다시 나누는 동안 방금 고른 칩을 보이고 첫 카드는 skeleton으로, 표 자리는 다시 나누는 중으로 두며 적용을 끈다`() {
        host.show(SettingsSamples.delimiterState(pendingDelimiter = ','))
        composeRule.mainClock.advanceTimeBy(LOADING_VISIBLE_MILLIS)

        chip("쉼표").assertIsSelected()
        composeRule.onNodeWithText("다시 나누고 있어요").assertIsDisplayed()
        composeRule.onNode(hasText("사과") and hasText("apple")).assertDoesNotExist()
        composeRule.onNodeWithText("적용").assertIsNotEnabled()
    }

    @Test
    fun `다시 나누는 동안에도 칩을 누를 수 있다`() {
        host.show(SettingsSamples.delimiterState(pendingDelimiter = ','))

        chip("세미콜론").performClick()

        host.intents shouldBe listOf(ImportDeckIntent.SelectDelimiter(';'))
    }

    @Test
    fun `파일에 열 이름이 적혀 있으면 체크 대신 그대로 쓴다는 안내를 보인다`() {
        val table = SettingsSamples.textTable.copy(layout = ParsedLayout(delimiter = "|", hasHeaderRow = false, columnsFromHeader = true))
        host.show(SettingsSamples.delimiterState(table))

        composeRule.onNodeWithText("파일에 적힌 열 이름을 그대로 써요").assertIsDisplayed()
        composeRule.onNodeWithText("첫 줄을 열 이름으로 쓰기").assertDoesNotExist()
        chip("세로선 |").assertIsSelected()
    }

    @Test
    fun `20열을 넘으면 보이는 열 수와 나머지 열 안내를 보인다`() {
        host.show(SettingsSamples.delimiterState(ImportTestTables.text(rows = 10, columns = 25)))
        val list = composeRule.onNode(hasScrollAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
        list.performScrollToNode(hasText("나머지 열도 카드 구성을 정할 때 고를 수 있어요."))

        composeRule.onNodeWithText("열 25개 중 20개, 10줄 중 처음 4줄").assertIsDisplayed()
        composeRule.onNodeWithText("나머지 열도 카드 구성을 정할 때 고를 수 있어요.").assertIsDisplayed()
    }

    @Test
    fun `큰 글자에서는 표 대신 줄별 카드로 보이고 카드 하나를 한 문장으로 읽는다`() {
        host.show(SettingsSamples.delimiterState(), fontScale = 1.3f)
        val card = hasContentDescription("1번째 줄, 열 1 사과, 열 2 apple")
        val list = composeRule.onNode(hasScrollAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))
        list.performScrollToNode(card)

        composeRule.onNode(card).assertIsDisplayed()
    }

    private companion object {
        const val LOADING_VISIBLE_MILLIS = 300L
        const val COLUMN_COUNT = 3
        const val THIRD_COLUMN_PEEK = 40f
        val TABLE_START_PADDING = 16.dp
    }
}
