package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.components.isHeading
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 문구는 Figma 화면의 표시 문구다. 팁의 장수만 주입한 상한을 따른다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class SourceStepTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        maxCards: Int = 10_000,
        parentHeight: Dp? = null,
    ) {
        composeRule.setContent {
            CaroTheme {
                Box(modifier = if (parentHeight == null) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(parentHeight)) {
                    SourceStep(
                        maxCards = maxCards,
                        onClickFile = {},
                        onClickPaste = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun formatRowText(label: String): String =
        composeRule
            .onNode(hasText(label))
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .joinToString("|") { text -> text.text }

    @Test
    fun `상한을 5,000으로 주입하면 팁 첫 줄도 5,000장이다`() {
        show(maxCards = 5_000)

        composeRule.onNodeWithText("한 번에 최대 5,000장까지 가져올 수 있어요.").assertExists()
        composeRule.onNodeWithText("한 번에 최대 10,000장까지 가져올 수 있어요.").assertDoesNotExist()
    }

    @Test
    fun `팁 제목과 나머지 세 줄을 보여준다`() {
        show()

        (listOf(TIP_LABEL) + FIXED_TIP_LINES).forEach { line -> composeRule.onNodeWithText(line).assertExists() }
    }

    @Test
    fun `가져오기 방법 두 가지의 제목과 설명을 보여준다`() {
        show()

        listOf(FILE_LABEL, FILE_HINT, PASTE_LABEL, PASTE_HINT).forEach { text ->
            composeRule.onNodeWithText(text).assertExists()
        }
    }

    @Test
    fun `가져올 수 있는 자료는 이름과 확장자 칩을 한 줄로 읽는다`() {
        show()

        composeRule.onNodeWithText(SUPPORTED_LABEL).assertExists()
        FORMAT_ROWS.map { (label, _) -> formatRowText(label) } shouldBe FORMAT_ROWS.map { (_, row) -> row }
    }

    @Test
    fun `안내 제목을 제목으로 읽히게 한다`() {
        show()

        composeRule.onNode(hasText(SUBTITLE) and isHeading).assertExists()
    }

    @Test
    fun `내용이 화면보다 길면 스크롤로 마지막 팁 줄까지 닿는다`() {
        show(parentHeight = 240.dp)
        composeRule.onNodeWithText(LAST_TIP_LINE).assertIsNotDisplayed()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(LAST_TIP_LINE))

        composeRule.onNodeWithText(LAST_TIP_LINE).assertIsDisplayed()
    }

    private companion object {
        const val SUBTITLE = "어떻게 가져올까요?"
        const val FILE_LABEL = "파일에서 가져오기"
        const val FILE_HINT = "Anki, 엑셀, 텍스트 파일을 골라요"
        const val PASTE_LABEL = "텍스트 붙여넣기"
        const val PASTE_HINT = "Quizlet 등에서 복사한 내용을 붙여 넣어요"
        const val SUPPORTED_LABEL = "이런 걸 가져올 수 있어요"
        const val TIP_LABEL = "TIP"
        const val LAST_TIP_LINE = "Anki 학습 기록은 가져오지 않아 모든 카드를 처음부터 학습해요."

        val FORMAT_ROWS =
            listOf(
                "Anki 덱 패키지" to "Anki 덱 패키지|.apkg",
                "Anki에서 내보낸 텍스트" to "Anki에서 내보낸 텍스트|.txt",
                "Quizlet에서 내보낸 텍스트" to "Quizlet에서 내보낸 텍스트|.txt|붙여넣기",
                "엑셀 단어장" to "엑셀 단어장|.xlsx",
            )
        val FIXED_TIP_LINES =
            listOf(
                "파일은 100MB까지 고를 수 있어요.",
                "이미지, 음성, 영상 같은 미디어는 가져오지 않아요.",
                LAST_TIP_LINE,
            )
    }
}
