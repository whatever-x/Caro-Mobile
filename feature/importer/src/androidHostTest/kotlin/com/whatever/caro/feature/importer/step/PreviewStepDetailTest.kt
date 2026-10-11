package com.whatever.caro.feature.importer.step

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.whatever.caro.feature.importer.component.isHeading
import com.whatever.caro.feature.importer.transform.ImportCard
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 카드 전체 보기 팝업. 문구는 Figma 프레임의 표시 문구다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class PreviewStepDetailTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host by lazy { PreviewStepHost(composeRule) }

    private fun card(card: ImportCard): SemanticsNodeInteraction = composeRule.onNode(cardItem(card))

    private fun inDialogNode(text: String): SemanticsNodeInteraction = composeRule.onNode(inDialog(hasText(text)))

    private fun frontTitle(): SemanticsNodeInteraction = composeRule.onNode(inDialog(hasText("앞면") and isHeading))

    private fun backTitle(): SemanticsNodeInteraction = composeRule.onNode(inDialog(hasText("뒷면") and isHeading))

    private fun closeButton(): SemanticsNodeInteraction = composeRule.onNode(inDialog(hasText("닫기") and hasClickAction()))

    private fun top(node: SemanticsNodeInteraction): Float = node.bounds().top.value

    private fun open(target: ImportCard) {
        card(target).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun `카드를 누르기 전에는 팝업이 없다`() {
        host.show()

        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun `카드를 누르면 전체 보기 팝업이 열린다`() {
        host.show(applyEdits = true)

        open(PreviewStepTables.SCHOOL)

        composeRule.onNode(isDialog()).assertIsDisplayed()
    }

    @Test
    fun `카드를 누르면 앞면과 뒷면 구역 제목이 heading인 팝업이 열린다`() {
        host.show(applyEdits = true)

        open(PreviewStepTables.SCHOOL)

        frontTitle().assertIsDisplayed()
        backTitle().assertIsDisplayed()
    }

    @Test
    fun `팝업은 하나만 열린다`() {
        host.show(applyEdits = true)

        open(PreviewStepTables.SCHOOL)

        composeRule.onAllNodes(isDialog()).assertCountEquals(1)
    }

    @Test
    fun `팝업의 앞면 구역 제목은 heading이다`() {
        host.show(detailCard = PreviewStepTables.JAPAN)

        frontTitle().assertIsDisplayed()
    }

    @Test
    fun `팝업의 뒷면 구역 제목은 heading이다`() {
        host.show(detailCard = PreviewStepTables.JAPAN)

        backTitle().assertIsDisplayed()
    }

    @Test
    fun `팝업의 heading은 앞면과 뒷면 둘뿐이다`() {
        host.show(detailCard = PreviewStepTables.JAPAN)

        composeRule.onAllNodes(inDialog(isHeading)).assertCountEquals(2)
    }

    @Test
    fun `앞면 구역은 카드 앞면 글을 보여준다`() {
        host.show(detailCard = PreviewStepTables.JAPAN)

        inDialogNode(PreviewStepTables.JAPAN.front).assertIsDisplayed()
    }

    @Test
    fun `뒷면 구역은 줄임 없이 카드 뒷면 전체 줄을 보여준다`() {
        val long = PreviewStepTables.card(index = 0, front = "단어", back = PreviewStepTables.LONG_BACK)
        host.show(preview = PreviewStepTables.preview(cards = listOf(long)), detailCard = long)

        val layout = inDialogNode(PreviewStepTables.LONG_BACK).textLayout()

        layout.lineCount shouldBe LONG_BACK_LINES
    }

    @Test
    fun `채운 빈 면 문구도 구역 본문으로 보여준다`() {
        host.show(detailCard = PreviewStepTables.MOUNT_FUJI)

        inDialogNode(PreviewStepTables.EMPTY_SIDE).assertIsDisplayed()
    }

    @Test
    fun `앞면 제목 앞면 글 뒷면 제목 뒷면 글 순서로 놓인다`() {
        host.show(detailCard = PreviewStepTables.JAPAN)

        val order =
            listOf(
                top(frontTitle()),
                top(inDialogNode(PreviewStepTables.JAPAN.front)),
                top(backTitle()),
                top(inDialogNode(PreviewStepTables.JAPAN.back)),
            )

        (order == order.sorted()) shouldBe true
    }

    @Test
    fun `열리면 앞면 제목에 포커스를 둔다`() {
        host.show(detailCard = PreviewStepTables.JAPAN)

        frontTitle().assertIsFocused()
    }

    @Test
    fun `팝업에 닫기 버튼이 있다`() {
        host.show(detailCard = PreviewStepTables.JAPAN)

        closeButton().assertIsDisplayed()
    }

    @Test
    fun `닫기 버튼을 누르면 닫기를 한 번 요청한다`() {
        host.show(detailCard = PreviewStepTables.JAPAN)

        closeButton().performClick()

        host.dismissDetailCount shouldBe 1
    }

    @Test
    fun `팝업 밖을 누르면 닫기를 한 번 요청한다`() {
        host.show(detailCard = PreviewStepTables.JAPAN)

        composeRule.onNode(isDialog()).performTouchInput { click(Offset(OUTSIDE_TAP_PX, OUTSIDE_TAP_PX)) }
        composeRule.waitForIdle()

        host.dismissDetailCount shouldBe 1
    }

    @Test
    fun `닫기 버튼으로 닫으면 연 카드 항목에 포커스를 돌려준다`() {
        host.show(applyEdits = true)
        open(PreviewStepTables.READ)

        closeButton().performClick()
        composeRule.waitForIdle()

        card(PreviewStepTables.READ).assertIsFocused()
    }

    @Test
    fun `닫아도 다른 카드 항목에는 포커스를 주지 않는다`() {
        host.show(applyEdits = true)
        open(PreviewStepTables.READ)

        closeButton().performClick()
        composeRule.waitForIdle()

        card(PreviewStepTables.SCHOOL).assertIsNotFocused()
    }

    @Test
    fun `팝업 밖을 눌러 닫아도 연 카드 항목에 포커스를 돌려준다`() {
        host.show(applyEdits = true)
        open(PreviewStepTables.READ)

        composeRule.onNode(isDialog()).performTouchInput { click(Offset(OUTSIDE_TAP_PX, OUTSIDE_TAP_PX)) }
        composeRule.waitForIdle()

        card(PreviewStepTables.READ).assertIsFocused()
    }

    @Test
    fun `다른 카드를 다시 열고 닫으면 이번에 연 카드에 포커스를 돌려준다`() {
        host.show(applyEdits = true)
        open(PreviewStepTables.SCHOOL)
        closeButton().performClick()
        composeRule.waitForIdle()

        open(PreviewStepTables.READ)
        closeButton().performClick()
        composeRule.waitForIdle()

        card(PreviewStepTables.READ).assertIsFocused()
    }

    private companion object {
        const val OUTSIDE_TAP_PX = 4f
        const val LONG_BACK_LINES = 12
    }
}
