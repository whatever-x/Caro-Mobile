package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.component.isHeading
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 문구는 Figma 프레임의 표시 문구다. 상한은 작은 값을 주입해 긴 글을 만들지 않고 경계를 확인한다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class PasteStepTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var parentHeight by mutableStateOf(400.dp)
    private var verticalPadding: Dp = 0.dp
    private var nextClicks = 0
    private val typed = mutableListOf<String>()

    private fun show(
        text: String = "",
        maxPasteChars: Int = 1_000,
    ) {
        composeRule.setContent {
            CaroTheme {
                verticalPadding = CaroTheme.spacing.l
                Box(modifier = Modifier.fillMaxWidth().height(parentHeight)) {
                    PasteStep(
                        text = text,
                        maxPasteChars = maxPasteChars,
                        onTextChange = { typed += it },
                        onNext = { nextClicks += 1 },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun field(): SemanticsNodeInteraction = composeRule.onNode(hasSetTextAction())

    private fun nextButton(): SemanticsNodeInteraction = composeRule.onNodeWithText(NEXT)

    private fun fieldHeight(): Dp = field().getUnclippedBoundsInRoot().height

    private fun tooLongReason(limitText: String) = "한 번에 ${limitText}자까지 붙여 넣을 수 있어요. 나눠서 가져와 주세요."

    @Test
    fun `상한을 넘는 글이면 다음 버튼이 꺼진다`() {
        show(text = "가".repeat(11), maxPasteChars = 10)

        nextButton().assertIsNotEnabled()
    }

    @Test
    fun `상한을 넘는 글이면 다음 버튼의 상태 설명에 이유가 들어간다`() {
        show(text = "가".repeat(11), maxPasteChars = 10)

        nextButton().fetchSemanticsNode().config[SemanticsProperties.StateDescription] shouldBe tooLongReason("10")
    }

    @Test
    fun `상한을 넘는 글이면 이유 줄이 보인다`() {
        show(text = "가".repeat(11), maxPasteChars = 10)

        composeRule.onNodeWithText(tooLongReason("10")).assertIsDisplayed()
    }

    @Test
    fun `이유 줄의 상한은 천 단위 구분 기호를 넣어 보여준다`() {
        show(text = "가".repeat(1_001), maxPasteChars = 1_000)

        composeRule.onNodeWithText(tooLongReason("1,000")).assertExists()
    }

    @Test
    fun `상한을 넘어도 입력한 글을 자르지 않는다`() {
        val text = "가".repeat(11)
        show(text = text, maxPasteChars = 10)

        field().assertTextEquals(text)
    }

    @Test
    fun `글자 수가 상한과 같으면 다음 버튼이 켜진다`() {
        show(text = "가".repeat(10), maxPasteChars = 10)

        nextButton().assertIsEnabled()
    }

    @Test
    fun `글자 수가 상한 이하이면 이유 줄이 없다`() {
        show(text = "가".repeat(10), maxPasteChars = 10)

        composeRule.onNode(hasText("붙여 넣을 수 있어요", substring = true)).assertDoesNotExist()
    }

    @Test
    fun `비어 있으면 다음 버튼이 꺼지고 이유 줄은 없다`() {
        show(text = "")

        nextButton().assertIsNotEnabled()
        composeRule.onNode(hasText("붙여 넣을 수 있어요", substring = true)).assertDoesNotExist()
    }

    @Test
    fun `다음 버튼을 누르면 다음을 한 번 요청한다`() {
        show(text = "학교\t학교")

        nextButton().performClick()

        nextClicks shouldBe 1
    }

    @Test
    fun `글을 입력하면 입력한 글을 알린다`() {
        show()

        field().performTextInput("학교")

        typed shouldBe listOf("학교")
    }

    @Test
    fun `제목과 안내 두 줄을 보여준다`() {
        show()

        composeRule.onNode(hasText(TITLE) and isHeading).assertIsDisplayed()
        listOf(ROW_GUIDE, COLUMN_GUIDE).forEach { guide -> composeRule.onNodeWithText(guide).assertIsDisplayed() }
    }

    @Test
    fun `짧은 글이어도 입력칸은 부모가 커진 만큼 함께 커진다`() {
        show(text = "짧은 글")
        val before = fieldHeight()

        composeRule.runOnIdle { parentHeight = 500.dp }
        composeRule.waitForIdle()
        val after = fieldHeight()

        (after.value - before.value) shouldBe (100f plusOrMinus 0.5f)
    }

    @Test
    fun `남는 높이가 모자라도 입력칸은 120dp 아래로 줄지 않는다`() {
        parentHeight = 200.dp
        show(text = "짧은 글")

        field().assertHeightIsEqualTo(120.dp - verticalPadding * 2)
    }

    private companion object {
        const val NEXT = "다음"
        const val TITLE = "복사한 내용을 붙여 넣어 주세요"
        const val ROW_GUIDE = "한 줄이 카드 한 장이 돼요."
        const val COLUMN_GUIDE = "칸은 탭, 쉼표, 세미콜론으로 나눠 주세요. 어느 칸을 앞뒷면에 넣을지는 뒤에서 골라요."
    }
}
