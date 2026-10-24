package com.whatever.caro.feature.importer.step

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.whatever.caro.feature.importer.step.DeckInfoStepHost.Companion.CONFIRM
import com.whatever.caro.feature.importer.step.DeckInfoStepHost.Companion.SUMMARY
import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 키보드가 열린 작은 화면을 흉내 내려고 줄어든 높이를 호스트 높이로 준다.
// 내용이 다 들어갈 때는 스크롤하지 않고, 모자랄 때만 스크롤하며, 가져오기 버튼은 어느 경우에도 보여야 한다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class DeckInfoStepLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = DeckInfoStepHost(composeRule)

    @Test
    fun `높이가 넉넉하면 내용은 스크롤하지 않는다`() {
        host.show(height = 800.dp)

        composeRule.onNodeWithText(CONFIRM).assertIsDisplayed()
        composeRule.verticalScrollMax() shouldBe 0f
    }

    @Test
    fun `키보드가 열려 높이가 모자라면 내용이 스크롤된다`() {
        host.show(height = SHORT_HEIGHT)

        composeRule.verticalScrollMax() shouldBeGreaterThan 0f
    }

    @Test
    fun `높이가 모자라도 가져오기 버튼은 보인다`() {
        host.show(height = SHORT_HEIGHT)

        composeRule.onNodeWithText(CONFIRM).assertIsDisplayed()
    }

    @Test
    fun `높이가 모자라면 스크롤해서 새 덱 안내 줄까지 볼 수 있다`() {
        host.show(height = SHORT_HEIGHT)

        composeRule.onNodeWithText(SUMMARY).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `가져오기 버튼은 내용이 짧아도 내용 바로 아래가 아니라 화면 아래쪽에 붙는다`() {
        host.show(height = 800.dp)

        val gap = composeRule.onNodeWithText(CONFIRM).bounds().top - composeRule.onNodeWithText(SUMMARY).bounds().bottom

        gap.value shouldBeGreaterThan 100f
    }

    @Test
    fun `글자를 200퍼센트로 키워도 가져오기 버튼은 보인다`() {
        host.show(height = 360.dp, fontScale = 2f)

        composeRule.onNodeWithText(CONFIRM).assertIsDisplayed()
    }

    @Test
    fun `글자를 200퍼센트로 키우면 스크롤해서 새 덱 안내 줄까지 볼 수 있다`() {
        host.show(height = 360.dp, fontScale = 2f)

        composeRule.onNodeWithText(SUMMARY).performScrollTo().assertIsDisplayed()
    }

    private companion object {
        val SHORT_HEIGHT = 300.dp
    }
}
