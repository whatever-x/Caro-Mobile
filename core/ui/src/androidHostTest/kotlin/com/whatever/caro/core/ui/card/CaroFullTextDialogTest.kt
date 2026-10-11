package com.whatever.caro.core.ui.card

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.whatever.caro.core.designsystem.themes.CaroTheme
import io.kotest.matchers.comparables.shouldBeLessThan
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
class CaroFullTextDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun showDialog(
        sections: List<CaroFullTextSection>,
        onDismiss: () -> Unit = {},
    ) {
        composeRule.setContent {
            CaroTheme {
                CaroFullTextDialog(sections = sections, onDismiss = onDismiss)
            }
        }
        composeRule.waitForIdle()
    }

    private fun showFrontAndBack(onDismiss: () -> Unit = {}) = showDialog(FRONT_AND_BACK, onDismiss)

    private fun showBodyOnly(onDismiss: () -> Unit = {}) =
        showDialog(listOf(CaroFullTextSection(title = null, body = BODY_ONLY)), onDismiss)

    private fun top(text: String): Float =
        composeRule
            .onNodeWithText(text)
            .getUnclippedBoundsInRoot()
            .top.value

    private fun headingNodes() = composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

    private fun SemanticsNodeInteraction.assertIsHeading(): SemanticsNodeInteraction =
        assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

    @Test
    fun `열면 다이얼로그 창을 띄운다`() {
        showFrontAndBack()

        composeRule.onNode(isDialog()).assertIsDisplayed()
    }

    @Test
    fun `구역 제목을 모두 보여준다`() {
        showFrontAndBack()

        composeRule.onNodeWithText(FRONT_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText(BACK_TITLE).assertIsDisplayed()
    }

    @Test
    fun `구역 본문을 모두 보여준다`() {
        showFrontAndBack()

        composeRule.onNodeWithText(FRONT_BODY).assertIsDisplayed()
        composeRule.onNodeWithText(BACK_BODY).assertIsDisplayed()
    }

    @Test
    fun `구역 제목은 같은 구역의 본문 위에 놓인다`() {
        showFrontAndBack()

        top(FRONT_TITLE) shouldBeLessThan top(FRONT_BODY)
    }

    @Test
    fun `구역은 전달한 순서대로 위에서 아래로 놓인다`() {
        showFrontAndBack()

        top(FRONT_BODY) shouldBeLessThan top(BACK_TITLE)
    }

    @Test
    fun `구역 제목은 heading으로 노출된다`() {
        showFrontAndBack()

        composeRule.onNodeWithText(FRONT_TITLE).assertIsHeading()
        composeRule.onNodeWithText(BACK_TITLE).assertIsHeading()
    }

    @Test
    fun `구역 본문은 heading으로 노출되지 않는다`() {
        showFrontAndBack()

        composeRule.onNodeWithText(FRONT_BODY).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Heading))
    }

    @Test
    fun `제목이 없는 구역도 본문을 보여준다`() {
        showBodyOnly()

        composeRule.onNodeWithText(BODY_ONLY).assertIsDisplayed()
    }

    @Test
    fun `제목이 없는 구역만 있으면 heading이 없다`() {
        showBodyOnly()

        headingNodes().assertCountEquals(0)
    }

    @Test
    fun `열리면 첫 제목에 포커스를 둔다`() {
        showFrontAndBack()

        composeRule.onNodeWithText(FRONT_TITLE).assertIsFocused()
    }

    @Test
    fun `열려도 둘째 제목에는 포커스를 두지 않는다`() {
        showFrontAndBack()

        composeRule.onNodeWithText(BACK_TITLE).assertIsNotFocused()
    }

    @Test
    fun `제목이 없으면 열릴 때 본문에 포커스를 둔다`() {
        showBodyOnly()

        composeRule.onNodeWithText(BODY_ONLY).assertIsFocused()
    }

    @Test
    fun `다이얼로그 밖을 누르면 onDismiss를 한 번 호출한다`() {
        var dismissCount = 0
        showFrontAndBack(onDismiss = { dismissCount += 1 })

        composeRule.onNode(isDialog()).performTouchInput { click(Offset(OUTSIDE_TAP_PX, OUTSIDE_TAP_PX)) }
        composeRule.waitForIdle()

        dismissCount shouldBe 1
    }

    @Test
    fun `다이얼로그 안의 본문을 눌러도 onDismiss를 호출하지 않는다`() {
        var dismissCount = 0
        showFrontAndBack(onDismiss = { dismissCount += 1 })

        composeRule.onNodeWithText(FRONT_BODY).performTouchInput { click() }
        composeRule.waitForIdle()

        dismissCount shouldBe 0
    }

    @Test
    fun `앞 구역이 화면보다 길어도 스크롤하면 다음 구역 본문을 볼 수 있다`() {
        showDialog(
            listOf(
                CaroFullTextSection(title = FRONT_TITLE, body = LONG_BODY),
                CaroFullTextSection(title = BACK_TITLE, body = BACK_BODY),
            ),
        )
        // 스크롤 전에는 보이지 않아야 내용이 한 화면에 들어가서 통과하는 일이 없다.
        composeRule.onNodeWithText(BACK_BODY).assertIsNotDisplayed()

        composeRule.onNodeWithText(BACK_BODY).performScrollTo()

        composeRule.onNodeWithText(BACK_BODY).assertIsDisplayed()
    }

    private companion object {
        const val FRONT_TITLE = "앞면"
        const val BACK_TITLE = "뒷면"
        const val FRONT_BODY = "apple"
        const val BACK_BODY = "사과"
        const val BODY_ONLY = "제목 없는 전체 글"
        const val LONG_BODY_LINES = 300
        const val OUTSIDE_TAP_PX = 4f
        val LONG_BODY = List(LONG_BODY_LINES) { "긴 본문 $it 줄" }.joinToString(separator = "\n")
        val FRONT_AND_BACK =
            listOf(
                CaroFullTextSection(title = FRONT_TITLE, body = FRONT_BODY),
                CaroFullTextSection(title = BACK_TITLE, body = BACK_BODY),
            )
    }
}
