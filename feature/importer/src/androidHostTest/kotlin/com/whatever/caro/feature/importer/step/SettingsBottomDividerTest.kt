package com.whatever.caro.feature.importer.step

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroColor
import com.whatever.caro.core.importparser.model.ParsedSheet
import com.whatever.caro.feature.importer.support.SettingsSamples
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
class SettingsBottomDividerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = SettingsScreenHost(composeRule)
    private val dividerColor = CaroColor.defaultColor().surface.tertiary
    private val longSheets =
        SettingsSamples.sheetState(
            SettingsSamples.sheetTable.copy(sheets = List(LONG_LIST) { index -> ParsedSheet(index, "시트 $index", 10) }),
        )

    /** Robolectric에서 captureToImage는 draw 대기에서 멈추므로 view를 직접 그려 읽는다. */
    private fun renderedPixels(): PixelMap {
        val view = (composeRule.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap.asImageBitmap().toPixelMap()
    }

    // 화면 가장자리 열에는 목록 카드나 버튼이 닿지 않으므로 이 열에서 구분선 색이 나오는 행이 구분선이다.
    private fun dividerRows(): List<Int> {
        val pixels = renderedPixels()
        return (0 until pixels.height).filter { y -> pixels[0, y] == dividerColor }
    }

    @Test
    fun `설정 화면은 내용이 다 보이면 하단 구분선을 숨긴다`() {
        host.show(SettingsSamples.sheetState())

        dividerRows().shouldBeEmpty()
    }

    @Test
    fun `설정 화면은 내용이 하단 고정 영역 아래로 더 있으면 구분선을 적용 버튼 위에 보인다`() {
        host.show(longSheets)
        val rows = dividerRows()
        val buttonTop =
            composeRule
                .onNodeWithText("적용")
                .fetchSemanticsNode()
                .boundsInRoot.top

        rows.size shouldBe with(composeRule.density) { 1.dp.roundToPx() }
        rows.max().toFloat() shouldBeLessThan buttonTop
    }

    @Test
    fun `설정 화면은 끝까지 스크롤해 위쪽 내용이 가려져도 구분선을 보인다`() {
        host.show(longSheets)
        val list = composeRule.onNode(hasScrollAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange))

        // 마지막 아이템이 보인 직후에는 목록이 아직 더 스크롤된다고 답하므로 한 번 더 밀어 끝에 닿게 한다.
        list.performScrollToNode(hasText("첫 줄을 열 이름으로 쓰기"))
        list.performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        dividerRows().shouldNotBeEmpty()
    }

    private companion object {
        const val LONG_LIST = 30
    }
}
