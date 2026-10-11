package com.whatever.caro.feature.importer.step

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.transform.ImportCard
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

// 필터 칩, 카드 목록, 카드 아래 배지, 위에 붙는 목록 머리. 문구는 Figma 프레임의 표시 문구다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class PreviewStepListTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host by lazy { PreviewStepHost(composeRule) }

    private fun chip(label: String): SemanticsNodeInteraction = composeRule.onNode(isTabRole and hasText(label))

    private fun chipLabelsLeftToRight(): List<String> =
        composeRule
            .onAllNodes(isTabRole)
            .fetchSemanticsNodes()
            .sortedBy { node -> node.positionInRoot.x }
            .map { node -> node.config[SemanticsProperties.Text].joinToString(separator = "") { text -> text.text } }

    private fun chipTops(): Set<Float> =
        composeRule
            .onAllNodes(isTabRole)
            .fetchSemanticsNodes()
            .map { node -> node.positionInRoot.y }
            .toSet()

    private fun card(card: ImportCard): SemanticsNodeInteraction = composeRule.onNode(cardItem(card))

    private fun top(node: SemanticsNodeInteraction): Float = node.bounds().top.value

    private fun badgesOf(card: ImportCard): List<String> {
        val readings = card(card).fetchSemanticsNode().readings()
        return ALL_BADGES.filter { badge -> readings.any { reading -> badge in reading } }
    }

    private fun showOne(card: ImportCard) {
        host.show(preview = PreviewStepTables.preview(cards = listOf(card)))
    }

    private fun swipeListUp() {
        composeRule.onNode(hasVerticalScrollRange).performTouchInput { swipeUp() }
        composeRule.waitForIdle()
    }

    @Test
    fun `필터 칩은 전체 빈 면 채움 잘린 내용 미디어 제거 순서로 놓인다`() {
        host.show()

        chipLabelsLeftToRight() shouldBe listOf("전체", "빈 면 채움", "잘린 내용", "미디어 제거")
    }

    @Test
    fun `필터 칩은 모두 한 줄에 놓인다`() {
        host.show()

        chipTops().size shouldBe 1
    }

    @Test
    fun `필터 칩은 하나만 고르는 탭 그룹이다`() {
        host.show()

        composeRule.onAllNodes(isTabRole).assertCountEquals(4)
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup)).assertCountEquals(1)
    }

    @Test
    fun `전체 필터면 전체 칩만 선택 상태다`() {
        host.show(filter = ImportPreviewFilter.All)

        chip("전체").assertIsSelected()
        chip("빈 면 채움").assertIsNotSelected()
        chip("잘린 내용").assertIsNotSelected()
        chip("미디어 제거").assertIsNotSelected()
    }

    @Test
    fun `잘린 내용 필터면 잘린 내용 칩만 선택 상태다`() {
        host.show(filter = ImportPreviewFilter.Truncated)

        chip("전체").assertIsNotSelected()
        chip("잘린 내용").assertIsSelected()
    }

    @Test
    fun `칩을 누르면 그 필터로 선택을 요청한다`() {
        host.show()

        chip("잘린 내용").performClick()

        host.filterRequests shouldBe listOf(ImportPreviewFilter.Truncated)
    }

    @Test
    fun `전체 칩을 누르면 전체로 선택을 요청한다`() {
        host.show(filter = ImportPreviewFilter.MediaRemoved)

        chip("전체").performClick()

        host.filterRequests shouldBe listOf(ImportPreviewFilter.All)
    }

    @Test
    fun `바뀐 카드가 없으면 전체 칩만 보인다`() {
        host.show(preview = PreviewStepTables.preview(cards = PreviewStepTables.plainCards(count = 5)))

        chipLabelsLeftToRight() shouldBe listOf("전체")
    }

    @Test
    fun `카드가 0장이면 전체 칩만 보인다`() {
        host.show(preview = PreviewStepTables.preview(cards = emptyList()))

        chipLabelsLeftToRight() shouldBe listOf("전체")
    }

    @Test
    fun `미디어를 지운 카드가 없으면 미디어 제거 칩을 숨긴다`() {
        host.show(preview = PreviewStepTables.preview(cards = listOf(PreviewStepTables.READ, PreviewStepTables.JAPAN)))

        chipLabelsLeftToRight() shouldBe listOf("전체", "빈 면 채움", "잘린 내용")
    }

    @Test
    fun `칩 줄은 가로 스크롤 컨테이너 하나다`() {
        host.show()

        composeRule.onAllNodes(hasHorizontalScrollRange).assertCountEquals(1)
    }

    @Test
    fun `칩이 화면 너비에 들어가면 칩 줄은 넘치지 않는다`() {
        host.show(preview = PreviewStepTables.preview(cards = listOf(PreviewStepTables.READ)))

        composeRule.onNode(hasHorizontalScrollRange).horizontalScrollMax() shouldBe 0f
    }

    @Test
    fun `큰 글자에서도 칩 넷을 한 줄에 두고 넘치는 만큼 가로로 스크롤한다`() {
        host.show(fontScale = 2f)

        composeRule.onAllNodes(isTabRole).assertCountEquals(4)
        chipTops().size shouldBe 1
        (composeRule.onNode(hasHorizontalScrollRange).horizontalScrollMax() > 0f) shouldBe true
    }

    @Test
    fun `카드는 앞면 글을 보여준다`() {
        host.show()

        composeRule.onNodeWithText(PreviewStepTables.SCHOOL.front, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `카드는 뒷면 글을 보여준다`() {
        host.show()

        composeRule.onNodeWithText(PreviewStepTables.SCHOOL.back, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `카드가 만 장이어도 화면에 보이는 카드만 그린다`() {
        host.show(preview = PreviewStepTables.preview(cards = PreviewStepTables.plainCards(count = MAX_CARDS_IN_PREVIEW)))

        val drawn = composeRule.onAllNodes(hasClickAction() and hasText("단어", substring = true)).fetchSemanticsNodes().size

        (drawn in 1..MAX_DRAWN_CARDS) shouldBe true
    }

    @Test
    fun `카드는 원본 순서대로 위에서 아래로 놓인다`() {
        host.show()

        val school = top(card(PreviewStepTables.SCHOOL))
        val read = top(card(PreviewStepTables.READ))
        val japan = top(card(PreviewStepTables.JAPAN))

        (school < read && read < japan) shouldBe true
    }

    @Test
    fun `앞면은 두 줄까지만 그리고 넘치면 말줄임한다`() {
        showOne(PreviewStepTables.card(index = 0, front = PreviewStepTables.LONG_FRONT))

        val layout = composeRule.onNodeWithText(PreviewStepTables.LONG_FRONT, useUnmergedTree = true).textLayout()

        layout.lineCount shouldBe 2
        layout.isLineEllipsized(1) shouldBe true
    }

    @Test
    fun `짧은 앞면은 한 줄만 쓴다`() {
        host.show()

        composeRule.onNodeWithText(PreviewStepTables.SCHOOL.front, useUnmergedTree = true).textLayout().lineCount shouldBe 1
    }

    @Test
    fun `뒷면은 세 줄까지만 그리고 넘치면 말줄임한다`() {
        showOne(PreviewStepTables.card(index = 0, back = PreviewStepTables.LONG_BACK))

        val layout = composeRule.onNodeWithText(PreviewStepTables.LONG_BACK, useUnmergedTree = true).textLayout()

        layout.lineCount shouldBe 3
        layout.isLineEllipsized(2) shouldBe true
    }

    @Test
    fun `뒷면이 세 줄이면 말줄임 없이 모두 그린다`() {
        host.show()

        val layout = composeRule.onNodeWithText(PreviewStepTables.SCHOOL.back, useUnmergedTree = true).textLayout()

        layout.lineCount shouldBe 3
        layout.isLineEllipsized(2) shouldBe false
    }

    @Test
    fun `카드는 누르는 버튼 역할이다`() {
        host.show()

        card(PreviewStepTables.SCHOOL).assert(isButtonRole)
    }

    @Test
    fun `카드를 누르면 그 카드로 전체 보기를 요청한다`() {
        host.show()

        card(PreviewStepTables.READ).performClick()

        host.cardClicks shouldBe listOf(PreviewStepTables.READ)
    }

    @Test
    fun `바뀐 것이 없는 카드는 배지가 없다`() {
        host.show()

        badgesOf(PreviewStepTables.SCHOOL) shouldBe emptyList()
    }

    @Test
    fun `뒷면을 채운 카드는 빈 면 채움 뒷면 배지를 보여준다`() {
        host.show()

        badgesOf(PreviewStepTables.READ) shouldBe listOf("빈 면 채움 · 뒷면")
    }

    @Test
    fun `뒷면이 잘린 카드는 잘린 내용 뒷면 배지를 보여준다`() {
        host.show()

        badgesOf(PreviewStepTables.JAPAN) shouldBe listOf("잘린 내용 · 뒷면")
    }

    @Test
    fun `앞면이 잘린 카드는 잘린 내용 앞면 배지를 보여준다`() {
        val frontTruncated = PreviewStepTables.card(index = 0, front = "友達", back = "친구", frontTruncated = true)
        showOne(frontTruncated)

        badgesOf(frontTruncated) shouldBe listOf("잘린 내용 · 앞면")
    }

    @Test
    fun `두 면이 모두 잘린 카드는 잘린 내용 앞뒷면 배지 하나를 보여준다`() {
        val bothTruncated = PreviewStepTables.card(index = 0, front = "友達", back = "친구", frontTruncated = true, backTruncated = true)
        showOne(bothTruncated)

        badgesOf(bothTruncated) shouldBe listOf("잘린 내용 · 앞뒷면")
    }

    @Test
    fun `미디어를 지운 카드는 미디어 제거 배지를 보여준다`() {
        showOne(PreviewStepTables.EAT)

        badgesOf(PreviewStepTables.EAT) shouldBe listOf("미디어 제거")
    }

    @Test
    fun `앞면을 채우고 뒷면이 잘리고 미디어를 지운 카드는 배지 셋을 모두 보여준다`() {
        showOne(PreviewStepTables.MOUNT_FUJI)

        badgesOf(PreviewStepTables.MOUNT_FUJI) shouldBe listOf("빈 면 채움 · 앞면", "잘린 내용 · 뒷면", "미디어 제거")
    }

    @Test
    fun `잘린 내용 필터는 잘린 카드만 목록에 둔다`() {
        host.show(filter = ImportPreviewFilter.Truncated)

        card(PreviewStepTables.JAPAN).assertIsDisplayed()
        card(PreviewStepTables.MOUNT_FUJI).assertIsDisplayed()
        card(PreviewStepTables.SCHOOL).assertDoesNotExist()
    }

    @Test
    fun `빈 면 채움 필터는 빈 면을 채운 카드만 목록에 둔다`() {
        host.show(filter = ImportPreviewFilter.EmptyReplaced)

        card(PreviewStepTables.READ).assertIsDisplayed()
        card(PreviewStepTables.MOUNT_FUJI).assertIsDisplayed()
        card(PreviewStepTables.FRIEND).assertIsDisplayed()
        card(PreviewStepTables.EAT).assertDoesNotExist()
    }

    @Test
    fun `미디어 제거 필터는 미디어를 지운 카드만 목록에 둔다`() {
        host.show(filter = ImportPreviewFilter.MediaRemoved)

        card(PreviewStepTables.EAT).assertIsDisplayed()
        card(PreviewStepTables.MOUNT_FUJI).assertIsDisplayed()
        card(PreviewStepTables.READ).assertDoesNotExist()
    }

    @Test
    fun `필터를 적용해도 요약 수치는 전체 기준 그대로다`() {
        host.show(filter = ImportPreviewFilter.Truncated)

        composeRule.onNodeWithText("8장").assertIsDisplayed()
    }

    @Test
    fun `처음 그릴 때는 필터 결과 장수를 알리지 않는다`() {
        host.show()

        composeRule.onAllNodes(isPoliteRegion and readsContaining("8장")).assertCountEquals(0)
    }

    @Test
    fun `필터를 바꾸면 필터 이름과 결과 장수를 알린다`() {
        host.show(applyEdits = true)

        chip("빈 면 채움").performClick()

        composeRule.onNode(isPoliteRegion and readsAs("빈 면 채움 3장")).assertExists()
    }

    @Test
    fun `전체로 바꾸면 전체 장수를 알린다`() {
        host.show(filter = ImportPreviewFilter.Truncated, applyEdits = true)

        chip("전체").performClick()

        composeRule.onNode(isPoliteRegion and readsAs("전체 8장")).assertExists()
    }

    @Test
    fun `스크롤해도 카드 목록 제목이 위에 붙어 남는다`() {
        host.show(preview = PreviewStepTables.longPreview())

        swipeListUp()

        composeRule.onNodeWithText("카드 목록").assertIsDisplayed()
        (top(composeRule.onNodeWithText("카드 목록")) < STICKY_TOP_LIMIT_DP) shouldBe true
    }

    @Test
    fun `스크롤해도 필터 칩이 위에 붙어 남는다`() {
        host.show(preview = PreviewStepTables.longPreview())

        swipeListUp()

        chip("전체").assertIsDisplayed()
    }

    @Test
    fun `위에 붙은 필터 칩도 누르면 선택을 요청한다`() {
        host.show(preview = PreviewStepTables.longPreview())
        swipeListUp()

        chip("잘린 내용").performClick()

        host.filterRequests shouldBe listOf(ImportPreviewFilter.Truncated)
    }

    @Test
    fun `글자 130퍼센트에서도 카드 목록 머리가 위에 붙는다`() {
        host.show(preview = PreviewStepTables.longPreview(), fontScale = 1.3f)

        swipeListUp()

        composeRule.onNodeWithText("카드 목록").assertIsDisplayed()
        (top(composeRule.onNodeWithText("카드 목록")) < STICKY_TOP_LIMIT_DP) shouldBe true
    }

    @Test
    fun `목록 머리의 높이는 붙기 전과 붙은 뒤가 같다`() {
        host.show(preview = PreviewStepTables.longPreview())
        val before = chip("전체").bounds().bottom.value - top(composeRule.onNodeWithText("카드 목록"))

        swipeListUp()
        val after = chip("전체").bounds().bottom.value - top(composeRule.onNodeWithText("카드 목록"))

        (abs(before - after) < HEIGHT_TOLERANCE_DP) shouldBe true
    }

    @Test
    fun `필터를 바꾸면 목록 머리가 맨 위에 붙도록 올라간다`() {
        host.show(applyEdits = true)
        (top(composeRule.onNodeWithText("카드 목록")) > STICKY_TOP_LIMIT_DP) shouldBe true

        chip("잘린 내용").performClick()
        composeRule.waitForIdle()

        (top(composeRule.onNodeWithText("카드 목록")) < STICKY_TOP_LIMIT_DP) shouldBe true
        composeRule.hasNoVisibleNode(hasText("가져올 카드")) shouldBe true
    }

    @Test
    fun `필터를 바꾸면 첫 카드가 필터 칩 바로 아래에 온다`() {
        host.show(applyEdits = true)

        chip("잘린 내용").performClick()
        composeRule.waitForIdle()

        val gap = top(card(PreviewStepTables.JAPAN)) - chip("전체").bounds().bottom.value
        (gap in 0f..FIRST_CARD_GAP_LIMIT_DP) shouldBe true
    }

    private companion object {
        val ALL_BADGES =
            listOf(
                "빈 면 채움 · 앞면",
                "빈 면 채움 · 뒷면",
                "잘린 내용 · 앞면",
                "잘린 내용 · 뒷면",
                "잘린 내용 · 앞뒷면",
                "미디어 제거",
            )
        const val MAX_CARDS_IN_PREVIEW = 10_000
        const val MAX_DRAWN_CARDS = 30
        const val STICKY_TOP_LIMIT_DP = 40f
        const val HEIGHT_TOLERANCE_DP = 1f
        const val FIRST_CARD_GAP_LIMIT_DP = 40f
    }
}
