package com.whatever.caro.feature.importer.step

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.step.SelectionStepTables.xlsx
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 구분선은 늘 1dp를 차지하고 색만 켜고 끈다. 그래서 위치가 아니라 화면 픽셀로 보인 여부를 읽는다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class StepBottomDividerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var tokenColor = Color.Unspecified

    private fun showSheetStep(
        sheetCount: Int,
        height: Dp,
    ) {
        composeRule.setContent {
            CaroTheme {
                Box(modifier = Modifier.size(393.dp, height)) {
                    SheetStep(
                        table = xlsx(sheets = List(sheetCount) { index -> "시트 ${index + 1}" to 100 }),
                        sheetIndex = 0,
                        hasHeaderRow = true,
                        maxCards = 10_000,
                        onSelectSheet = {},
                        onChangeHeaderRow = {},
                        onNext = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun showDivider(isVisible: Boolean) {
        composeRule.setContent {
            CaroTheme {
                tokenColor = CaroTheme.color.surface.tertiary
                Box(modifier = Modifier.testTag(DIVIDER_TAG)) { StepBottomDivider(isVisible = isVisible) }
            }
        }
        composeRule.waitForIdle()
    }

    /** Robolectric에서 captureToImage는 draw 대기에서 멈추므로 view를 직접 그려 읽는다. */
    private fun renderedPixels(): PixelMap {
        val view = (composeRule.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap.asImageBitmap().toPixelMap()
    }

    // 화면 가장자리 열에는 카드나 버튼이 닿지 않으므로 이 열에서 구분선 색이 나오는 행이 구분선이다.
    private fun dividerRows(): List<Int> {
        val pixels = renderedPixels()
        return (0 until pixels.height).filter { y -> pixels[0, y] == DIVIDER_COLOR }
    }

    private fun dividerThicknessPx(): Int = with(composeRule.density) { 1.dp.roundToPx() }

    private fun nextButtonTop(): Float =
        composeRule
            .onNodeWithText(NEXT)
            .fetchSemanticsNode()
            .boundsInRoot.top

    @Test
    fun `구분선을 숨겨도 1dp 높이를 차지한다`() {
        showDivider(isVisible = false)

        composeRule.onNodeWithTag(DIVIDER_TAG).assertHeightIsEqualTo(1.dp)
    }

    @Test
    fun `보이는 구분선은 surface tertiary 색이다`() {
        showDivider(isVisible = true)

        renderedPixels()[0, 0] shouldBe tokenColor
        tokenColor shouldBe DIVIDER_COLOR
    }

    @Test
    fun `숨긴 구분선은 아무것도 그리지 않는다`() {
        showDivider(isVisible = false)

        dividerRows().shouldBeEmpty()
    }

    @Test
    fun `선택 단계는 내용이 다 보이면 구분선을 숨긴다`() {
        showSheetStep(sheetCount = 3, height = 700.dp)

        dividerRows().shouldBeEmpty()
    }

    @Test
    fun `선택 단계는 내용이 고정 영역 아래로 더 있으면 구분선을 다음 버튼 위에 보인다`() {
        showSheetStep(sheetCount = 40, height = 400.dp)
        val rows = dividerRows()

        rows.size shouldBe dividerThicknessPx()
        rows.max().toFloat() shouldBeLessThan nextButtonTop()
    }

    @Test
    fun `선택 단계는 끝까지 스크롤해 위쪽 내용이 가려져도 구분선을 보인다`() {
        showSheetStep(sheetCount = 40, height = 400.dp)

        // 마지막 아이템이 보인 직후에는 목록이 아직 더 스크롤된다고 답하므로 한 번 더 밀어 끝에 닿게 한다.
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(HEADER_ROW))
        composeRule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        dividerRows().shouldNotBeEmpty()
    }

    @Test
    fun `텍스트 확인은 내용이 다 보이면 구분선을 숨긴다`() {
        TextCheckStepHost(composeRule).show()

        dividerRows().shouldBeEmpty()
    }

    @Test
    fun `텍스트 확인은 최대 글자 크기에서 내용이 넘치면 구분선을 보인다`() {
        TextCheckStepHost(composeRule).show(width = 360.dp, height = 640.dp, fontScale = 2f)

        dividerRows().shouldNotBeEmpty()
    }

    @Test
    fun `미리보기는 카드가 다 보이면 구분선을 숨긴다`() {
        PreviewStepHost(
            composeRule,
        ).show(preview = PreviewStepTables.preview(cards = PreviewStepTables.plainCards(count = 2, firstIndex = 0)))

        dividerRows().shouldBeEmpty()
    }

    @Test
    fun `미리보기는 카드가 아래로 더 있으면 구분선을 보인다`() {
        PreviewStepHost(composeRule).show(preview = PreviewStepTables.longPreview())

        dividerRows().shouldNotBeEmpty()
    }

    private companion object {
        const val DIVIDER_TAG = "stepBottomDivider"
        val DIVIDER_COLOR = Color(0xFFEDF0FE)
        const val HEADER_ROW = "첫 줄은 제목"
        const val NEXT = "다음"
    }
}
