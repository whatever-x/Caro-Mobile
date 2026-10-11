package com.whatever.caro.feature.importer.step

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.whatever.caro.feature.importer.mvi.SavingStatus
import com.whatever.caro.feature.importer.step.SavingStepHost.Companion.STOP
import com.whatever.caro.feature.importer.step.SavingStepHost.Companion.progress
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 면 색은 core:designsystem의 CaroCtaButtonTest가 고정한 Danger와 비활성 색이다. 평소는 연한 빨강 버튼이고 멈추는 중은 연한 파랑 버튼이다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class SavingStepButtonStyleTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = SavingStepHost(composeRule)

    @Test
    fun `보통 상태에서 가져오기 멈추기 버튼은 위험 색 면이다`() {
        host.show(progress(status = SavingStatus.Saving))

        composeRule.onNodeWithText(STOP).surfaceColor() shouldBe DANGEROUS_SURFACE
    }

    @Test
    fun `연결을 다시 시도하는 중에도 가져오기 멈추기 버튼은 위험 색 면이다`() {
        host.show(progress(status = SavingStatus.Retrying))

        composeRule.onNodeWithText(STOP).surfaceColor() shouldBe DANGEROUS_SURFACE
    }

    @Test
    fun `멈추는 중에는 가져오기 멈추기 버튼이 비활성 색 면이다`() {
        host.show(progress(status = SavingStatus.Stopping))

        composeRule.onNodeWithText(STOP).surfaceColor() shouldBe DISABLED_SURFACE
    }

    private companion object {
        val DANGEROUS_SURFACE = Color(0xFFFFF2F1)
        val DISABLED_SURFACE = Color(0xFFEDF0FE)
    }
}
