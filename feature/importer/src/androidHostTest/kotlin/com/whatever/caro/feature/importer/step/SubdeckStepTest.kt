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
import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.component.isHeading
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.floats.shouldBeLessThanOrEqual
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 문구는 Figma 프레임의 표시 문구다. 장수 상한은 주입한 값을 따른다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class SubdeckStepTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val toggledIds = mutableListOf<String>()
    private var toggleAllClicks = 0
    private var nextClicks = 0

    private val isPoliteLiveRegion = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    private fun show(
        table: ParsedTable = SelectionStepTables.apkg(),
        selectedIds: PersistentSet<String> = persistentSetOf("1", "2"),
        maxCards: Int = 10_000,
        width: Dp = 393.dp,
        height: Dp = 700.dp,
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density = baseDensity.density, fontScale = fontScale)) {
                CaroTheme {
                    Box(modifier = Modifier.size(width, height)) {
                        SubdeckStep(
                            table = table,
                            selectedIds = selectedIds,
                            maxCards = maxCards,
                            onToggleSubdeck = { id -> toggledIds += id },
                            onToggleAll = { toggleAllClicks += 1 },
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

    private fun summaryTexts(): List<String> =
        composeRule
            .onNode(isPoliteLiveRegion and hasText(SELECTED_LABEL))
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .map { text -> text.text }

    private fun nextButton(): SemanticsNodeInteraction = composeRule.onNodeWithText(NEXT)

    private fun textNode(text: String): SemanticsNodeInteraction = composeRule.onNodeWithText(text, useUnmergedTree = true)

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    private fun SemanticsNodeInteraction.toggleState(): ToggleableState = fetchSemanticsNode().config[SemanticsProperties.ToggleableState]

    private fun overLimitTable(): ParsedTable = SelectionStepTables.apkg(subdecks = listOf("단어" to 9_800, "문법" to 2_600))

    // 요약 합계 1,514장이 다른 어떤 줄의 장수와도 겹치지 않아 글자 노드를 하나로 찾을 수 있다.
    private fun threeSubdeckTable(): ParsedTable =
        SelectionStepTables.apkg(subdecks = listOf("단어" to 1_204, "문법" to 310, MEDIUM_NAME to 520))

    private fun manySubdecksTable(count: Int): ParsedTable =
        SelectionStepTables.apkg(subdecks = List(count) { index -> "Day ${(index + 1).toString().padStart(3, '0')}" to 10 })

    @Test
    fun `파일 이름을 부제로 보여준다`() {
        show()

        composeRule.onNodeWithText(SelectionStepTables.APKG_NAME).assertIsDisplayed()
    }

    @Test
    fun `안내 제목을 제목으로 읽히게 한다`() {
        show()

        composeRule.onNode(hasText(HEADING) and isHeading).assertIsDisplayed()
    }

    @Test
    fun `고른 하위 덱은 한 덱으로 합친다는 안내를 보여준다`() {
        show()

        composeRule.onNodeWithText(MERGE_HINT).assertIsDisplayed()
    }

    @Test
    fun `하위 덱 이름은 공통 상위 덱 이름을 뺀 뒤 보여준다`() {
        show()

        row("단어").assertExists()
        composeRule.onNodeWithText("JLPT N5::단어").assertDoesNotExist()
    }

    @Test
    fun `하위 덱 줄은 노트 수를 장 단위로 보여준다`() {
        show()

        rowTexts("단어") shouldContain "1,204장"
    }

    @Test
    fun `전체 선택 줄은 모든 하위 덱의 합을 보여준다`() {
        show(table = threeSubdeckTable(), selectedIds = persistentSetOf("1", "3"))

        rowTexts(SELECT_ALL) shouldContain "2,034장"
    }

    @Test
    fun `노트 수 안내 두 줄을 목록 아래에 보여준다`() {
        show()

        listOf(NOTE_COUNT_HINT, EMPTY_NOTES_HINT).forEach { hint -> composeRule.onNodeWithText(hint).assertIsDisplayed() }
    }

    @Test
    fun `하위 덱 줄은 전체 선택 줄보다 32dp 들여 쓴다`() {
        show()
        val allLeft = textNode(SELECT_ALL).fetchSemanticsNode().boundsInRoot.left
        val subdeckLeft = textNode("단어").fetchSemanticsNode().boundsInRoot.left

        ((subdeckLeft - allLeft) / composeRule.density.density) shouldBe (32f plusOrMinus 2f)
    }

    @Test
    fun `고른 하위 덱 줄은 켜지고 고르지 않은 줄은 꺼진다`() {
        show(table = threeSubdeckTable(), selectedIds = persistentSetOf("1"))

        listOf("단어", "문법").map { name -> row(name).toggleState() } shouldBe listOf(ToggleableState.On, ToggleableState.Off)
    }

    @Test
    fun `하위 덱을 모두 고르면 전체 선택 줄이 켜진다`() {
        show(selectedIds = persistentSetOf("1", "2"))

        row(SELECT_ALL).toggleState() shouldBe ToggleableState.On
    }

    @Test
    fun `하위 덱을 하나도 고르지 않으면 전체 선택 줄이 꺼진다`() {
        show(selectedIds = persistentSetOf())

        row(SELECT_ALL).toggleState() shouldBe ToggleableState.Off
    }

    @Test
    fun `일부만 고르면 전체 선택 줄이 일부 선택 상태다`() {
        show(selectedIds = persistentSetOf("1"))

        row(SELECT_ALL).toggleState() shouldBe ToggleableState.Indeterminate
    }

    @Test
    fun `일부만 고르면 전체 선택 줄의 상태 설명이 일부 선택됨이다`() {
        show(selectedIds = persistentSetOf("1"))

        row(SELECT_ALL).fetchSemanticsNode().config[SemanticsProperties.StateDescription] shouldBe "일부 선택됨"
    }

    @Test
    fun `덱 없음만 골라도 전체 선택 줄은 꺼진다`() {
        show(selectedIds = persistentSetOf(ParsedSubdeck.NO_DECK_ID))

        row(SELECT_ALL).toggleState() shouldBe ToggleableState.Off
    }

    @Test
    fun `덱 없음 수가 0이면 덱 없음 줄이 없다`() {
        show(table = SelectionStepTables.apkg(noDeckCount = 0))

        composeRule.onNodeWithText(NO_DECK).assertDoesNotExist()
    }

    @Test
    fun `덱 없음 수가 0이면 카드가 없는 노트 설명도 없다`() {
        show(table = SelectionStepTables.apkg(noDeckCount = 0))

        composeRule.onNodeWithText(NO_DECK_HINT).assertDoesNotExist()
    }

    @Test
    fun `덱 없음 수가 있으면 줄에 설명과 개수를 보여준다`() {
        show(table = SelectionStepTables.apkg(noDeckCount = 3))

        rowTexts(NO_DECK) shouldContainExactlyInAnyOrder listOf(NO_DECK, NO_DECK_HINT, "3개")
    }

    @Test
    fun `덱 없음 줄은 하위 덱을 고른 상태에서도 기본으로 꺼져 있다`() {
        show(selectedIds = persistentSetOf("1", "2"))

        row(NO_DECK).toggleState() shouldBe ToggleableState.Off
    }

    @Test
    fun `덱 없음 줄은 하위 덱 줄 아래에 있다`() {
        show()

        val lastSubdeckBottom = row("문법").fetchSemanticsNode().boundsInRoot.bottom
        val noDeckTop = row(NO_DECK).fetchSemanticsNode().boundsInRoot.top

        lastSubdeckBottom shouldBeLessThanOrEqual noDeckTop
    }

    @Test
    fun `덱 없음 줄을 누르면 덱 없음 id로 선택 변경을 요청한다`() {
        show()

        row(NO_DECK).performClick()

        toggledIds shouldBe listOf(ParsedSubdeck.NO_DECK_ID)
    }

    @Test
    fun `하위 덱 줄은 체크박스 역할의 toggleable이다`() {
        show()

        row("단어").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
        row("단어").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState))
    }

    @Test
    fun `하위 덱 줄의 높이는 48dp 이상이다`() {
        show()

        row("단어").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `전체 선택 줄과 덱 없음 줄의 높이도 48dp 이상이다`() {
        show()

        listOf(SELECT_ALL, NO_DECK).forEach { label -> row(label).assertHeightIsAtLeast(48.dp) }
    }

    @Test
    fun `체크 줄마다 toggleable은 줄 하나뿐이고 체크박스는 따로 눌리지 않는다`() {
        show()

        composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState), useUnmergedTree = true)
            .assertCountEquals(4)
    }

    @Test
    fun `하위 덱 줄을 누르면 그 하위 덱 id로 선택 변경을 요청한다`() {
        show()

        row("문법").performClick()

        toggledIds shouldBe listOf("2")
    }

    @Test
    fun `전체 선택 줄을 누르면 전체 선택 변경만 요청한다`() {
        show()

        row(SELECT_ALL).performClick()

        listOf(toggleAllClicks, toggledIds.size) shouldBe listOf(1, 0)
    }

    @Test
    fun `다음 버튼을 누르면 다음을 한 번 요청한다`() {
        show()

        nextButton().performClick()

        nextClicks shouldBe 1
    }

    @Test
    fun `선택 요약은 라벨과 합계와 상한을 한 번에 읽는다`() {
        show(selectedIds = persistentSetOf("1", "2"))

        summaryTexts() shouldBe listOf(SELECTED_LABEL, "1,514장", "/ 최대 10,000장")
    }

    @Test
    fun `선택 요약의 합계는 고른 하위 덱의 합이다`() {
        show(table = threeSubdeckTable(), selectedIds = persistentSetOf("1", "3"))

        summaryTexts() shouldContain "1,724장"
    }

    @Test
    fun `선택 요약의 상한은 주입한 값을 따른다`() {
        show(maxCards = 5_000)

        summaryTexts() shouldContain "/ 최대 5,000장"
    }

    @Test
    fun `선택 요약은 상한 이하여도 polite live region이다`() {
        show()

        composeRule.onNode(isPoliteLiveRegion and hasText(SELECTED_LABEL)).assertExists()
    }

    @Test
    fun `합계가 상한과 같으면 초과로 보지 않고 다음 버튼이 켜진다`() {
        show(maxCards = 1_514)

        summaryTexts() shouldContain "1,514장"
        nextButton().assertIsEnabled()
    }

    @Test
    fun `합계가 상한보다 1장 많으면 요약에 초과가 붙는다`() {
        show(maxCards = 1_513)

        summaryTexts() shouldContain "1,514장 (초과)"
    }

    @Test
    fun `상한을 넘으면 요약 합계에 초과가 붙는다`() {
        show(table = overLimitTable())

        summaryTexts() shouldBe listOf(SELECTED_LABEL, "12,400장 (초과)", "/ 최대 10,000장")
    }

    @Test
    fun `상한을 넘어도 선택 요약은 polite live region이다`() {
        show(table = overLimitTable())

        composeRule.onNode(isPoliteLiveRegion and hasText("12,400장 (초과)")).assertExists()
    }

    @Test
    fun `상한을 넘으면 다음 버튼이 꺼진다`() {
        show(table = overLimitTable())

        nextButton().assertIsNotEnabled()
    }

    @Test
    fun `상한을 넘으면 다음 버튼의 상태 설명에 이유가 들어간다`() {
        show(table = overLimitTable())

        nextButton().fetchSemanticsNode().config[SemanticsProperties.StateDescription] shouldBe REASON
    }

    @Test
    fun `상한을 넘으면 다음 버튼 위에 이유 줄이 보인다`() {
        show(table = overLimitTable())

        composeRule.onNodeWithText(REASON).assertIsDisplayed()
    }

    @Test
    fun `상한 이하이면 이유 줄이 없다`() {
        show()

        composeRule.onNodeWithText(REASON).assertDoesNotExist()
    }

    @Test
    fun `하위 덱을 하나도 고르지 않으면 다음 버튼이 꺼진다`() {
        show(selectedIds = persistentSetOf())

        nextButton().assertIsNotEnabled()
    }

    @Test
    fun `보통 글자 크기에서는 라벨과 합계가 한 줄에 놓인다`() {
        show(table = threeSubdeckTable(), selectedIds = persistentSetOf("1", "2"))
        val label = textNode(SELECTED_LABEL).fetchSemanticsNode().boundsInRoot
        val amount = textNode("1,514장").fetchSemanticsNode().boundsInRoot

        (label.top < amount.bottom && amount.top < label.bottom) shouldBe true
    }

    @Test
    fun `글자 130퍼센트에서는 라벨과 합계와 상한이 위아래로 쌓인다`() {
        show(table = threeSubdeckTable(), selectedIds = persistentSetOf("1", "2"), fontScale = 1.3f)

        assertSummaryStacked(amount = "1,514장")
    }

    @Test
    fun `최대 글자 크기에서는 상한 초과 요약도 위아래로 쌓인다`() {
        show(table = overLimitTable(), width = 360.dp, height = 640.dp, fontScale = 2f)

        assertSummaryStacked(amount = "12,400장 (초과)")
    }

    @Test
    fun `최대 글자 크기에서도 다음 버튼과 이유 줄이 보이고 이유는 줄이지 않고 줄바꿈한다`() {
        show(table = overLimitTable(), width = 360.dp, height = 640.dp, fontScale = 2f)

        nextButton().assertIsDisplayed()
        val reason = composeRule.onNodeWithText(REASON)
        reason.assertIsDisplayed()
        reason.textLayout().lineCount shouldBeGreaterThan 1
        reason.textLayout().hasVisualOverflow shouldBe false
    }

    @Test
    fun `최대 글자 크기에서도 목록은 고정 영역 위에서 스크롤로 끝 줄까지 닿고 다음 버튼은 계속 보인다`() {
        show(table = overLimitTable(), width = 360.dp, height = 640.dp, fontScale = 2f)

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(NO_DECK))

        row(NO_DECK).assertIsDisplayed()
        nextButton().assertIsDisplayed()
    }

    @Test
    fun `긴 하위 덱 이름은 3줄까지 보이고 마지막 줄이 말줄임으로 끝난다`() {
        show(table = SelectionStepTables.apkg(subdecks = listOf("단어" to 1_204, LONG_NAME to 520)), selectedIds = persistentSetOf("1", "2"))

        val layout = textNode(LONG_NAME_PREFIX, substring = true).textLayout()

        listOf(layout.lineCount, layout.isLineEllipsized(2)) shouldBe listOf(3, true)
    }

    @Test
    fun `200자를 넘는 하위 덱 이름은 그리기 전에 200자 이내로 줄여 말줄임을 붙인다`() {
        show(table = SelectionStepTables.apkg(subdecks = listOf("단어" to 1_204, LONG_NAME to 520)), selectedIds = persistentSetOf("1", "2"))

        val shown =
            textNode(LONG_NAME_PREFIX, substring = true)
                .fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .single()
                .text

        shown.length shouldBeLessThanOrEqual 200
        shown shouldEndWith "…"
    }

    @Test
    fun `목록은 세로 스크롤 컨테이너 하나뿐이다`() {
        show()

        composeRule.onAllNodes(hasScrollAction()).assertCountEquals(1)
    }

    @Test
    fun `하위 덱이 많으면 보이는 줄만 그린다`() {
        show(table = manySubdecksTable(count = 200), selectedIds = persistentSetOf())

        val drawnRows = composeRule.onAllNodes(hasText("Day ", substring = true)).fetchSemanticsNodes().size

        drawnRows shouldBeGreaterThan 0
        drawnRows shouldBeLessThan 200
    }

    @Test
    fun `높이가 모자라면 스크롤로 마지막 안내 줄까지 닿고 요약과 다음 버튼은 계속 보인다`() {
        show(table = manySubdecksTable(count = 40), selectedIds = persistentSetOf(), height = 400.dp)

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(EMPTY_NOTES_HINT))

        composeRule.onNodeWithText(EMPTY_NOTES_HINT).assertIsDisplayed()
        nextButton().assertIsDisplayed()
        composeRule.onNode(isPoliteLiveRegion and hasText(SELECTED_LABEL)).assertIsDisplayed()
    }

    private fun assertSummaryStacked(amount: String) {
        val label = textNode(SELECTED_LABEL).fetchSemanticsNode().boundsInRoot
        val amountBounds = textNode(amount).fetchSemanticsNode().boundsInRoot
        val limit = textNode("/ 최대 10,000장").fetchSemanticsNode().boundsInRoot

        // 줄 경계의 반올림 오차 1px는 허용한다.
        label.bottom shouldBeLessThanOrEqual (amountBounds.top + 1f)
        amountBounds.bottom shouldBeLessThanOrEqual (limit.top + 1f)
    }

    private fun textNode(
        text: String,
        substring: Boolean,
    ): SemanticsNodeInteraction = composeRule.onNode(hasText(text, substring = substring), useUnmergedTree = true)

    private companion object {
        const val HEADING = "가져올 하위 덱을 골라 주세요"
        const val MERGE_HINT = "고른 하위 덱은 한 덱으로 합쳐요."
        const val SELECT_ALL = "전체 선택"
        const val NO_DECK = "덱 없음"
        const val NO_DECK_HINT = "카드가 없는 노트예요"
        const val NOTE_COUNT_HINT = "노트 1개가 카드 1장이 되어 Anki 카드 수와 다를 수 있어요."
        const val EMPTY_NOTES_HINT = "앞뒤가 모두 빈 노트는 미리보기에서 빠져요."
        const val SELECTED_LABEL = "선택한 카드"
        const val REASON = "고른 하위 덱을 줄이거나 Anki에서 하위 덱별로 나눠 내보내 주세요."
        const val NEXT = "다음"
        const val LONG_NAME_PREFIX = "긴하위덱긴하위덱"
        val LONG_NAME = "긴하위덱".repeat(75)
        const val MEDIUM_NAME = "Day 01 · 기초 동사와 형용사 활용(て형, た형, ない형)과 자주 틀리는 조사 연습 문제 모음, 복습 퀴즈"
    }
}
