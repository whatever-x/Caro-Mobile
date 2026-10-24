package com.whatever.caro.feature.importer.step

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.whatever.caro.feature.importer.step.SavingStepHost.Companion.DECK_NAME
import com.whatever.caro.feature.importer.step.SavingStepHost.Companion.STOP
import com.whatever.caro.feature.importer.step.SavingStepHost.Companion.WARNING
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
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class SavingStepLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = SavingStepHost(composeRule)

    @Test
    fun `높이가 넉넉하면 내용이 버튼 위 남는 높이의 가운데에 놓인다`() {
        host.show(height = 800.dp)

        val contentCenter = (composeRule.onNodeWithText(DECK_NAME).bounds().top + composeRule.onNodeWithText(WARNING).bounds().bottom) / 2
        val areaCenter = composeRule.onNodeWithText(STOP).bounds().top / 2

        abs(contentCenter.value - areaCenter.value) shouldBeLessThan CENTER_TOLERANCE
    }

    @Test
    fun `가져오기 멈추기 버튼은 내용 아래 화면 아래쪽에 붙는다`() {
        host.show(height = 800.dp)

        val gap = composeRule.onNodeWithText(STOP).bounds().top - composeRule.onNodeWithText(WARNING).bounds().bottom

        gap.value shouldBeGreaterThan 100f
    }

    @Test
    fun `높이가 넉넉하면 내용은 스크롤하지 않는다`() {
        host.show(height = 800.dp)

        composeRule.onNodeWithText(STOP).assertIsDisplayed()
        composeRule.verticalScrollMax() shouldBe 0f
    }

    @Test
    fun `높이가 모자라면 내용이 스크롤된다`() {
        host.show(height = SHORT_HEIGHT)

        composeRule.verticalScrollMax() shouldBeGreaterThan 0f
    }

    @Test
    fun `높이가 모자라도 가져오기 멈추기 버튼은 보인다`() {
        host.show(height = SHORT_HEIGHT)

        composeRule.onNodeWithText(STOP).assertIsDisplayed()
    }

    @Test
    fun `높이가 모자라면 스크롤해서 안내 문구까지 볼 수 있다`() {
        host.show(height = SHORT_HEIGHT)

        composeRule.onNodeWithText(WARNING).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `글자를 200퍼센트로 키워도 가져오기 멈추기 버튼은 보인다`() {
        host.show(height = LARGE_FONT_HEIGHT, fontScale = 2f)

        composeRule.onNodeWithText(STOP).assertIsDisplayed()
    }

    @Test
    fun `글자를 200퍼센트로 키우면 스크롤해서 안내 문구까지 볼 수 있다`() {
        host.show(height = LARGE_FONT_HEIGHT, fontScale = 2f)

        composeRule.onNodeWithText(WARNING).performScrollTo().assertIsDisplayed()
    }

    private companion object {
        val SHORT_HEIGHT = 160.dp
        val LARGE_FONT_HEIGHT = 300.dp

        // 위로 붙은 배치와 가운데 배치의 차이는 수백 dp라 이 값이면 가운데 여부가 가려진다. 버튼 위아래 여백 차이는 허용한다.
        const val CENTER_TOLERANCE = 48f
    }
}
