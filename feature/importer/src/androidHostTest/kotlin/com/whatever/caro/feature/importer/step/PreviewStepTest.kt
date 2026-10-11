package com.whatever.caro.feature.importer.step

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.height
import com.whatever.caro.core.importparser.model.ParseWarning
import com.whatever.caro.core.importparser.model.ParseWarningCode
import com.whatever.caro.feature.importer.component.isHeading
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

// 요약 카드, 경고 줄, 안내 줄. 문구는 Figma 프레임의 표시 문구다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class PreviewStepTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host by lazy { PreviewStepHost(composeRule) }

    private fun top(node: SemanticsNodeInteraction): Float = node.bounds().top.value

    private fun centerY(node: SemanticsNodeInteraction): Float = node.bounds().let { bounds -> bounds.top.value + bounds.height.value / 2 }

    // 필터 칩과 카드에도 같은 글이 있으므로 누를 수 없는 글자만 요약 줄로 본다.
    private fun summaryText(text: String): SemanticsMatcher = hasText(text) and hasClickAction().not()

    private fun summaryNode(text: String): SemanticsNodeInteraction = composeRule.onNode(summaryText(text))

    private fun showWithWarning(
        code: String,
        count: Int? = null,
    ) {
        host.show(preview = PreviewStepTables.preview(warnings = listOf(ParseWarning(code = code, count = count))))
    }

    @Test
    fun `요약 카드는 가져올 카드 라벨을 보여준다`() {
        host.show()

        composeRule.onNodeWithText("가져올 카드").assertIsDisplayed()
    }

    @Test
    fun `요약 카드는 가져올 장수를 보여준다`() {
        host.show()

        composeRule.onNodeWithText("8장").assertIsDisplayed()
    }

    @Test
    fun `가져올 장수 제목은 라벨과 장수를 한 문장으로 읽는다`() {
        host.show()

        composeRule.onNode(isHeading and readsAs("가져올 카드 8장")).assertIsDisplayed()
    }

    @Test
    fun `상한을 넘으면 가져올 장수 제목은 라벨과 상한을 한 문장으로 읽는다`() {
        host.show(preview = PreviewStepTables.preview(limitExceeded = true))

        composeRule.onNode(isHeading and readsAs("가져올 카드 10,000장 넘음")).assertIsDisplayed()
    }

    @Test
    fun `가져올 장수는 라벨 아래에 놓인다`() {
        host.show()

        (top(composeRule.onNodeWithText("가져올 카드")) < top(composeRule.onNodeWithText("8장"))) shouldBe true
    }

    @Test
    fun `가져올 장수는 천 단위 쉼표를 붙인다`() {
        host.show(preview = PreviewStepTables.preview(cards = PreviewStepTables.plainCards(1_512)))

        composeRule.onNodeWithText("1,512장").assertIsDisplayed()
    }

    @Test
    fun `가져올 카드가 하나도 없으면 0장을 보여준다`() {
        host.show(preview = PreviewStepTables.preview(cards = emptyList()))

        // 빈 면 채움과 잘린 내용 수치도 0장이므로 가져올 장수 제목으로 찾는다.
        composeRule.onNode(hasText("0장") and isHeading).assertIsDisplayed()
    }

    @Test
    fun `제외한 카드 수를 장 단위로 보여준다`() {
        host.show()

        summaryNode("제외 (앞뒤가 모두 빈 카드)").assertIsDisplayed()
        composeRule.onNodeWithText("7장").assertIsDisplayed()
    }

    @Test
    fun `제외한 카드 수도 천 단위 쉼표를 붙인다`() {
        host.show(preview = PreviewStepTables.preview(cards = emptyList(), excluded = 1_514))

        composeRule.onNodeWithText("1,514장").assertIsDisplayed()
    }

    @Test
    fun `빈 면을 채운 카드 수를 장 단위로 보여준다`() {
        host.show()

        summaryNode("빈 면 채움").assertIsDisplayed()
        composeRule.onNodeWithText("3장").assertIsDisplayed()
    }

    @Test
    fun `잘린 내용이 있는 카드 수를 장 단위로 보여준다`() {
        host.show()

        summaryNode("잘린 내용 (500자 초과)").assertIsDisplayed()
        composeRule.onNodeWithText("2장").assertIsDisplayed()
    }

    @Test
    fun `지운 미디어 수는 개 단위로 보여준다`() {
        host.show()

        summaryNode("미디어 제거").assertIsDisplayed()
        composeRule.onNodeWithText("3개").assertIsDisplayed()
    }

    @Test
    fun `수치는 이름과 같은 줄의 오른쪽에 놓인다`() {
        host.show()

        val pairs =
            listOf(
                "제외 (앞뒤가 모두 빈 카드)" to "7장",
                "빈 면 채움" to "3장",
                "잘린 내용 (500자 초과)" to "2장",
                "미디어 제거" to "3개",
            )

        // 수치 줄이 하나로 합쳐져 있어도 이름과 수치를 따로 보려고 병합하지 않은 트리에서 찾는다. 카드 목록보다 위인 첫 노드가 요약 줄이다.
        pairs.forEach { (label, value) ->
            val labelNode = composeRule.onAllNodes(hasText(label), useUnmergedTree = true).onFirst()
            val valueNode = composeRule.onAllNodes(hasText(value), useUnmergedTree = true).onFirst()
            val sameLine = abs(centerY(labelNode) - centerY(valueNode)) < SAME_LINE_TOLERANCE_DP
            (sameLine && valueNode.bounds().left > labelNode.bounds().right) shouldBe true
        }
    }

    @Test
    fun `상한을 넘으면 가져올 장수 자리에 상한과 넘음을 보여준다`() {
        host.show(preview = PreviewStepTables.preview(limitExceeded = true))

        composeRule.onNodeWithText("10,000장 넘음").assertIsDisplayed()
    }

    @Test
    fun `상한을 넘으면 읽은 만큼 센 장수는 보여주지 않는다`() {
        host.show(preview = PreviewStepTables.preview(limitExceeded = true))

        composeRule.onNodeWithText("8장").assertDoesNotExist()
    }

    @Test
    fun `상한을 넘어도 아래 수치는 그대로 보여준다`() {
        host.show(preview = PreviewStepTables.preview(limitExceeded = true))

        composeRule.onNodeWithText("7장").assertIsDisplayed()
        composeRule.onNodeWithText("3개").assertIsDisplayed()
    }

    @Test
    fun `성능 때문에 상한이 낮아지면 넘음 문구도 그 상한을 따른다`() {
        host.show(preview = PreviewStepTables.preview(limitExceeded = true), maxCards = 5_000)

        composeRule.onNodeWithText("5,000장 넘음").assertIsDisplayed()
    }

    @Test
    fun `카드는 덱을 만든 뒤 고칠 수 있다는 안내를 보여준다`() {
        host.show()

        composeRule.onNodeWithText(EDIT_LATER_HINT).assertIsDisplayed()
    }

    @Test
    fun `고칠 수 있다는 안내는 요약 수치 아래에 놓인다`() {
        host.show()

        (top(summaryNode("미디어 제거")) < top(composeRule.onNodeWithText(EDIT_LATER_HINT))) shouldBe true
    }

    @Test
    fun `카드 목록 제목은 고칠 수 있다는 안내 아래에 놓인다`() {
        host.show()

        (top(composeRule.onNodeWithText(EDIT_LATER_HINT)) < top(composeRule.onNodeWithText("카드 목록"))) shouldBe true
    }

    @Test
    fun `엑셀 숫자 서식 경고를 보여준다`() {
        showWithWarning(ParseWarningCode.XLSX_NUMBER_FORMAT_RAW)

        composeRule.onNodeWithText("숫자 서식 일부는 원래 값으로 가져와요.").assertIsDisplayed()
    }

    @Test
    fun `인코딩 대체 경고를 보여준다`() {
        showWithWarning(ParseWarningCode.ENCODING_FALLBACK_CP949)

        composeRule.onNodeWithText(CP949_WARNING).assertIsDisplayed()
    }

    @Test
    fun `칸 수가 다른 줄 경고는 줄 수를 문구에 넣는다`() {
        showWithWarning(ParseWarningCode.TEXT_INCONSISTENT_COLUMNS, count = 3)

        composeRule.onNodeWithText(COLUMNS_WARNING).assertIsDisplayed()
    }

    @Test
    fun `값이 없는 수식 경고는 수식 수를 문구에 넣는다`() {
        showWithWarning(ParseWarningCode.XLSX_FORMULA_WITHOUT_VALUE, count = 12)

        composeRule.onNodeWithText("계산 결과가 저장되지 않은 수식 12개는 빈 칸으로 가져와요.").assertIsDisplayed()
    }

    @Test
    fun `항목 수가 맞지 않는 노트 경고는 노트 수를 문구에 넣는다`() {
        showWithWarning(ParseWarningCode.APKG_FIELD_COUNT_MISMATCH, count = 4)

        composeRule.onNodeWithText("항목 수가 맞지 않는 노트 4개는 빈 항목으로 맞췄어요.").assertIsDisplayed()
    }

    @Test
    fun `이미지 가리기 카드 경고는 카드 수를 문구에 넣는다`() {
        showWithWarning(ParseWarningCode.APKG_IMAGE_OCCLUSION_EXCLUDED, count = 40)

        composeRule.onNodeWithText("이미지 가리기 카드 40장은 가져오지 않아요.").assertIsDisplayed()
    }

    @Test
    fun `경고 줄은 요약 수치 아래 고칠 수 있다는 안내 위에 놓인다`() {
        showWithWarning(ParseWarningCode.XLSX_NUMBER_FORMAT_RAW)

        val lastStat = top(summaryNode("미디어 제거"))
        val warning = top(composeRule.onNodeWithText("숫자 서식 일부는 원래 값으로 가져와요."))
        val hint = top(composeRule.onNodeWithText(EDIT_LATER_HINT))

        (lastStat < warning && warning < hint) shouldBe true
    }

    @Test
    fun `경고가 여럿이면 경고 코드 표 순서대로 한 줄씩 쌓는다`() {
        host.show(
            preview =
                PreviewStepTables.preview(
                    warnings =
                        listOf(
                            ParseWarning(ParseWarningCode.XLSX_NUMBER_FORMAT_RAW),
                            ParseWarning(ParseWarningCode.TEXT_INCONSISTENT_COLUMNS, count = 3),
                            ParseWarning(ParseWarningCode.ENCODING_FALLBACK_CP949),
                        ),
                ),
        )

        val cp949 = top(composeRule.onNodeWithText(CP949_WARNING))
        val columns = top(composeRule.onNodeWithText(COLUMNS_WARNING))
        val numberFormat = top(composeRule.onNodeWithText("숫자 서식 일부는 원래 값으로 가져와요."))

        (cp949 < columns && columns < numberFormat) shouldBe true
    }

    @Test
    fun `서버 전용 경고는 화면에 보이지 않는다`() {
        host.show(preview = PreviewStepTables.preview(warnings = listOf(ParseWarning("ROWS_OMITTED", count = 777))))

        composeRule.onNodeWithText("777", substring = true).assertDoesNotExist()
    }

    @Test
    fun `세로 스크롤 컨테이너는 요약부터 카드까지 하나다`() {
        host.show(preview = PreviewStepTables.longPreview())

        composeRule.onAllNodes(hasVerticalScrollRange).assertCountEquals(1)
    }

    @Test
    fun `요약 카드도 카드와 함께 스크롤되어 올라간다`() {
        host.show(preview = PreviewStepTables.longPreview())
        composeRule.onNodeWithText("가져올 카드").assertIsDisplayed()

        composeRule.onNode(hasVerticalScrollRange).performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        composeRule.hasNoVisibleNode(hasText("가져올 카드")) shouldBe true
    }

    private companion object {
        const val EDIT_LATER_HINT = "카드는 덱을 만든 뒤 덱 상세에서 고칠 수 있어요."
        const val CP949_WARNING = "한국어(CP949) 파일로 읽었어요. 글자가 깨졌다면 UTF-8로 저장해 다시 골라 주세요."
        const val COLUMNS_WARNING = "칸 수가 다른 줄 3개는 빈 칸을 채워 맞췄어요."
        const val SAME_LINE_TOLERANCE_DP = 6f
    }
}
