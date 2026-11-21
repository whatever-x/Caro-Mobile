package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.components.isHeading
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 문구는 Figma 화면의 표시 문구다. 상한은 작은 값을 주입해 긴 글을 만들지 않고 경계를 확인한다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class PasteStepTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var parentHeight by mutableStateOf(400.dp)
    private var verticalPadding: Dp = 0.dp

    private val isPoliteLiveRegion = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    private fun show(
        text: String = "",
        maxPasteChars: Int = 1_000,
        isOverflowed: Boolean = false,
        isNextEnabled: Boolean = text.isNotEmpty() && !isOverflowed,
    ) {
        composeRule.setContent {
            CaroTheme {
                verticalPadding = CaroTheme.spacing.l
                Box(modifier = Modifier.fillMaxWidth().height(parentHeight)) {
                    PasteStep(
                        text = text,
                        maxPasteChars = maxPasteChars,
                        isOverflowed = isOverflowed,
                        isNextEnabled = isNextEnabled,
                        onTextChange = {},
                        onNext = {},
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
    fun `길이를 넘으면 안내 목록을 숨기고 버튼 위 사유 줄은 없다`() {
        show(text = "가".repeat(11), maxPasteChars = 10, isOverflowed = true)

        GUIDES.forEach { guide -> composeRule.onNodeWithText(guide).assertDoesNotExist() }
        composeRule.onAllNodes(hasText(tooLongReason("10"))).fetchSemanticsNodes().size shouldBe 1
    }

    @Test
    fun `오류 문구는 polite live region이라 스크린 리더가 한 번 알린다`() {
        show(text = "가".repeat(11), maxPasteChars = 10, isOverflowed = true)

        composeRule.onNodeWithText(tooLongReason("10")).assert(isPoliteLiveRegion)
    }

    @Test
    fun `오류가 없어도 live region 노드는 글자 없이 남아 있어 오류가 생기면 글자만 바뀐다`() {
        show(text = "학교")

        val texts = composeRule.onNode(isPoliteLiveRegion).fetchSemanticsNode().config[SemanticsProperties.Text]
        texts.joinToString("") { text -> text.text } shouldBe ""
    }

    @Test
    fun `길이를 넘으면 다음 버튼의 상태 설명에 이유가 들어간다`() {
        show(text = "가".repeat(11), maxPasteChars = 10, isOverflowed = true)

        nextButton().fetchSemanticsNode().config[SemanticsProperties.StateDescription] shouldBe tooLongReason("10")
    }

    @Test
    fun `길이를 넘어도 입력한 글을 자르지 않는다`() {
        val text = "가".repeat(11)
        show(text = text, maxPasteChars = 10, isOverflowed = true)

        field().assertTextEquals(text)
    }

    @Test
    fun `넘치지 않았으면 다음 버튼이 켜지고 오류 문구 대신 안내 세 줄이 보인다`() {
        show(text = "가".repeat(10), maxPasteChars = 10)

        nextButton().assertIsEnabled()
        composeRule.onNode(hasText("붙여 넣을 수 있어요", substring = true)).assertDoesNotExist()
        GUIDES.forEach { guide -> composeRule.onNodeWithText(guide).assertIsDisplayed() }
    }

    @Test
    fun `제목을 제목으로 읽히게 한다`() {
        show()

        composeRule.onNode(hasText(TITLE) and isHeading).assertIsDisplayed()
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
    fun `오류가 없으면 남는 높이가 모자라도 입력칸은 120dp 아래로 줄지 않는다`() {
        parentHeight = 200.dp
        show(text = "짧은 글")

        field().assertHeightIsEqualTo(120.dp - verticalPadding * 2)
    }

    @Test
    fun `오류 상태에서는 남는 높이가 모자라면 입력칸이 120dp 아래로 줄어 오류 문구가 부모 안에 들어온다`() {
        parentHeight = 260.dp
        show(text = "가".repeat(11), maxPasteChars = 10, isOverflowed = true)

        val error = composeRule.onNodeWithText(tooLongReason("10")).getUnclippedBoundsInRoot()

        fieldHeight() shouldBeLessThan 120.dp - verticalPadding * 2
        field().getUnclippedBoundsInRoot().bottom shouldBeLessThan error.top
        error.bottom shouldBeLessThan 260.dp
    }

    @Test
    fun `입력칸은 붙여 넣을 내용을 안내하는 이름을 가진다`() {
        show()

        field().fetchSemanticsNode().config[SemanticsProperties.ContentDescription] shouldBe listOf(TITLE)
    }

    private companion object {
        const val NEXT = "다음"
        const val TITLE = "복사한 내용을 붙여 넣어 주세요"
        val GUIDES =
            listOf(
                "한 줄이 카드 한 장이 돼요.",
                "단어와 뜻 사이는 탭 키 간격, 쉼표(,), 세미콜론(;) 중 하나로 나눠 주세요.",
                "앞뒷면은 나중에 바꿀 수 있어요.",
            )
    }
}
