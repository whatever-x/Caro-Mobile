package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.component.isHeading
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 문구는 Figma 프레임의 표시 문구다. 팁의 장수만 주입한 상한을 따른다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class SourceStepTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var fileClicks = 0
    private var pasteClicks = 0

    private fun show(
        maxCards: Int = 10_000,
        parentHeight: Dp? = null,
    ) {
        composeRule.setContent {
            CaroTheme {
                Box(
                    modifier =
                        if (parentHeight == null) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier.fillMaxWidth().height(parentHeight)
                        },
                ) {
                    SourceStep(
                        maxCards = maxCards,
                        onClickFile = { fileClicks += 1 },
                        onClickPaste = { pasteClicks += 1 },
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `팁 첫 줄의 장수는 주입한 상한 10,000을 따른다`() {
        show(maxCards = 10_000)

        composeRule.onNodeWithText("한 번에 최대 10,000장까지 가져올 수 있어요.").assertExists()
    }

    @Test
    fun `팁 첫 줄의 장수는 주입한 상한 5,000을 따른다`() {
        show(maxCards = 5_000)

        composeRule.onNodeWithText("한 번에 최대 5,000장까지 가져올 수 있어요.").assertExists()
    }

    @Test
    fun `상한을 5,000으로 주입하면 10,000장 문구는 나오지 않는다`() {
        show(maxCards = 5_000)

        composeRule.onNodeWithText("한 번에 최대 10,000장까지 가져올 수 있어요.").assertDoesNotExist()
    }

    @Test
    fun `팁의 나머지 세 줄은 상한과 무관한 고정 문구다`() {
        show(maxCards = 5_000)

        FIXED_TIP_LINES.forEach { line -> composeRule.onNodeWithText(line).assertExists() }
    }

    @Test
    fun `선택 카드 두 개의 제목과 설명을 보여준다`() {
        show()

        listOf(FILE_LABEL, FILE_HINT, PASTE_LABEL, PASTE_HINT).forEach { text ->
            composeRule.onNodeWithText(text).assertExists()
        }
    }

    @Test
    fun `지원하는 자료 네 가지 형식의 이름을 보여준다`() {
        show()

        (listOf(SUPPORTED_LABEL) + FORMAT_LABELS).forEach { text ->
            composeRule.onNodeWithText(text).assertExists()
        }
    }

    @Test
    fun `소제목을 제목으로 읽히게 한다`() {
        show()

        composeRule.onNode(hasText(SUBTITLE) and isHeading).assertExists()
    }

    @Test
    fun `파일 카드를 누르면 파일 선택만 요청한다`() {
        show()

        composeRule.onNodeWithText(FILE_LABEL).performClick()

        listOf(fileClicks, pasteClicks) shouldBe listOf(1, 0)
    }

    @Test
    fun `붙여넣기 카드를 누르면 붙여넣기만 요청한다`() {
        show()

        composeRule.onNodeWithText(PASTE_LABEL).performClick()

        listOf(fileClicks, pasteClicks) shouldBe listOf(0, 1)
    }

    @Test
    fun `내용이 화면보다 길면 스크롤로 마지막 팁 줄까지 닿는다`() {
        show(parentHeight = 240.dp)
        composeRule.onNodeWithText(LAST_TIP_LINE).assertIsNotDisplayed()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(LAST_TIP_LINE))

        composeRule.onNodeWithText(LAST_TIP_LINE).assertIsDisplayed()
    }

    private companion object {
        const val SUBTITLE = "가져올 자료를 골라 주세요"
        const val FILE_LABEL = "파일에서 가져오기"
        const val FILE_HINT = ".apkg, .txt, .csv, .tsv, .xlsx 파일을 골라요"
        const val PASTE_LABEL = "텍스트 붙여넣기"
        const val PASTE_HINT = "Quizlet 등에서 복사한 내용을 붙여 넣어요"
        const val SUPPORTED_LABEL = "지원하는 자료"
        const val LAST_TIP_LINE = "Anki 학습 기록은 가져오지 않아 모든 카드를 처음부터 학습해요."

        val FORMAT_LABELS = listOf("Anki 덱 패키지", "Anki 텍스트 내보내기", "Quizlet 내보내기 텍스트", "엑셀 단어장")
        val FIXED_TIP_LINES =
            listOf(
                "파일은 100MB까지 고를 수 있어요.",
                "이미지, 음성, 영상은 가져오지 않아요.",
                LAST_TIP_LINE,
            )
    }
}
