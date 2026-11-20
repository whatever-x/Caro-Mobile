package com.whatever.caro.feature.importer.step

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.whatever.caro.feature.importer.mvi.SavingStatus
import com.whatever.caro.feature.importer.step.SavingStepHost.Companion.DECK_NAME
import com.whatever.caro.feature.importer.step.SavingStepHost.Companion.STOP
import com.whatever.caro.feature.importer.step.SavingStepHost.Companion.WARNING
import com.whatever.caro.feature.importer.step.SavingStepHost.Companion.progress
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

// 평소에는 내용이 가운데에 놓이고 스크롤하지 않으며, 공간이 모자랄 때만 스크롤한다. 가져오기 멈추기 버튼은 어느 경우에도 고정이다.
// 버튼 면 색은 core:designsystem의 CaroCtaButtonTest가 고정한 Danger와 비활성 색이다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class SavingStepLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = SavingStepHost(composeRule)

    @Test
    fun `높이가 넉넉하면 내용이 버튼 위 남는 높이의 가운데에 놓이고 스크롤하지 않는다`() {
        host.show(height = 800.dp)

        val contentCenter = (composeRule.onNodeWithText(DECK_NAME).bounds().top + composeRule.onNodeWithText(WARNING).bounds().bottom) / 2
        val areaCenter = composeRule.onNodeWithText(STOP).bounds().top / 2

        abs(contentCenter.value - areaCenter.value) shouldBeLessThan CENTER_TOLERANCE
        composeRule.verticalScrollMax() shouldBe 0f
    }

    @Test
    fun `높이가 모자라면 내용이 스크롤되고 가져오기 멈추기 버튼은 보인다`() {
        host.show(height = SHORT_HEIGHT)

        composeRule.verticalScrollMax() shouldBeGreaterThan 0f
        composeRule.onNodeWithText(STOP).assertIsDisplayed()
        composeRule.onNodeWithText(WARNING).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `글자를 200퍼센트로 키워도 버튼은 보이고 스크롤해서 안내 문구까지 볼 수 있다`() {
        host.show(height = LARGE_FONT_HEIGHT, fontScale = 2f)

        composeRule.onNodeWithText(STOP).assertIsDisplayed()
        composeRule.onNodeWithText(WARNING).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `보통 상태의 가져오기 멈추기 버튼은 위험 색 면이다`() {
        host.show(progress(status = SavingStatus.Saving))

        composeRule.onNodeWithText(STOP).surfaceColor() shouldBe DANGEROUS_SURFACE
    }

    @Test
    fun `멈추는 중의 가져오기 멈추기 버튼은 비활성 색 면이다`() {
        host.show(progress(status = SavingStatus.Stopping))

        composeRule.onNodeWithText(STOP).surfaceColor() shouldBe DISABLED_SURFACE
    }

    private companion object {
        val SHORT_HEIGHT = 160.dp
        val LARGE_FONT_HEIGHT = 300.dp

        // 위로 붙은 배치와 가운데 배치의 차이는 수백 dp라 이 값이면 가운데 여부가 가려진다. 버튼 위아래 여백 차이는 허용한다.
        const val CENTER_TOLERANCE = 48f
        val DANGEROUS_SURFACE = Color(0xFFFFF2F1)
        val DISABLED_SURFACE = Color(0xFFEDF0FE)
    }
}
