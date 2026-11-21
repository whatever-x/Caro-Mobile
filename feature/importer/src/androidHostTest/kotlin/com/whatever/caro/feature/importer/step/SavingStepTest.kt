package com.whatever.caro.feature.importer.step

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.whatever.caro.feature.importer.mvi.SavingStatus
import com.whatever.caro.feature.importer.step.SavingStepHost.Companion.FINISHING
import com.whatever.caro.feature.importer.step.SavingStepHost.Companion.RETRYING
import com.whatever.caro.feature.importer.step.SavingStepHost.Companion.SAVING
import com.whatever.caro.feature.importer.step.SavingStepHost.Companion.STOP
import com.whatever.caro.feature.importer.step.SavingStepHost.Companion.STOPPING
import com.whatever.caro.feature.importer.step.SavingStepHost.Companion.WARNING
import com.whatever.caro.feature.importer.step.SavingStepHost.Companion.progress
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 문구는 Figma 프레임의 표시 문구다. 막대의 모양(흐름, 정지)은 SavingProgressStyleTest와 스크린샷이 맡는다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class SavingStepTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = SavingStepHost(composeRule)

    @Test
    fun `첫 응답 전에는 저장 장수가 0 나누기 전체 장수다`() {
        host.show(progress(savedCards = 0))

        composeRule.onNodeWithText("0 / 1,512장").assertIsDisplayed()
    }

    @Test
    fun `멈추거나 앱을 닫으면 저장한 카드까지만 남는다는 안내를 보여준다`() {
        host.show()

        composeRule.onNodeWithText(WARNING).assertIsDisplayed()
    }

    @Test
    fun `연결을 다시 시도하는 중이면 다시 시도 문구가 보통 문구를 대신한다`() {
        host.show(progress(status = SavingStatus.Retrying))

        composeRule.onNodeWithText(RETRYING).assertIsDisplayed()
        composeRule.onNodeWithText(SAVING).assertDoesNotExist()
    }

    @Test
    fun `서버가 같은 묶음을 처리 중이면 마무리 문구가 보통 문구를 대신한다`() {
        host.show(progress(status = SavingStatus.WaitingForServer))

        composeRule.onNodeWithText(FINISHING).assertIsDisplayed()
        composeRule.onNodeWithText(SAVING).assertDoesNotExist()
    }

    @Test
    fun `멈추는 중이면 저장한 장수를 담은 멈추는 중 문구가 보통 문구를 대신한다`() {
        host.show(progress(savedCards = 1_000, status = SavingStatus.Stopping))

        composeRule.onNodeWithText(STOPPING).assertIsDisplayed()
        composeRule.onNodeWithText(SAVING).assertDoesNotExist()
    }

    @Test
    fun `저장한 카드가 없을 때 멈추는 중 문구의 장수는 0이다`() {
        host.show(progress(savedCards = 0, status = SavingStatus.Stopping))

        composeRule.onNodeWithText("멈추고 있어요. 0장까지 저장했어요.").assertIsDisplayed()
    }

    @Test
    fun `멈추는 중에도 저장 장수 줄은 그대로 보인다`() {
        host.show(progress(savedCards = 1_000, status = SavingStatus.Stopping))

        composeRule.onNodeWithText("1,000 / 1,512장").assertIsDisplayed()
    }

    @Test
    fun `상태 문구는 바뀔 때마다 polite live region으로 알린다`() {
        host.show(progress(status = SavingStatus.Saving))

        val polite =
            listOf(SavingStatus.Saving to SAVING, SavingStatus.Retrying to RETRYING, SavingStatus.WaitingForServer to FINISHING)
                .plus(SavingStatus.Stopping to STOPPING)
                .map { (status, text) ->
                    host.update(progress(status = status))
                    isPoliteRegion.matches(composeRule.onNodeWithText(text).fetchSemanticsNode())
                }

        polite shouldBe List(4) { true }
    }

    @Test
    fun `상태 문구는 제목이라 heading으로 읽는다`() {
        host.show()

        composeRule.onNodeWithText(SAVING).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun `첫 응답 전에는 진행 막대를 진행률 없는 진행 중으로 읽는다`() {
        host.show(progress(savedCards = 0))

        composeRule.onNode(hasProgressRange).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo] shouldBe
            ProgressBarRangeInfo.Indeterminate
    }

    @Test
    fun `첫 응답 전에는 진행 막대를 1512장 중 0장 저장됨으로 읽는다`() {
        host.show(progress(savedCards = 0))

        composeRule.onNode(hasProgressRange).assert(readsContaining("1,512장 중 0장 저장됨"))
    }

    @Test
    fun `동작 줄이기가 켜져 있어도 첫 응답 전 진행 막대는 진행률 없는 진행 중으로 읽는다`() {
        host.show(progress(savedCards = 0), reduceMotion = true)

        composeRule.onNode(hasProgressRange).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo] shouldBe
            ProgressBarRangeInfo.Indeterminate
    }

    @Test
    fun `첫 응답 뒤에는 진행 막대가 저장한 비율을 진행률로 준다`() {
        host.show(progress(savedCards = 1_000))

        composeRule.onNode(hasProgressRange).progressFraction() shouldBe (1_000f / 1_512f plusOrMinus 0.001f)
    }

    @Test
    fun `전부 저장했으면 진행 막대의 진행률이 1이다`() {
        host.show(progress(savedCards = 1_512))

        composeRule.onNode(hasProgressRange).progressFraction() shouldBe (1f plusOrMinus 0.001f)
    }

    @Test
    fun `첫 응답 뒤에는 진행 막대를 1512장 중 1000장 저장됨으로 읽고 바뀌면 알린다`() {
        host.show(progress(savedCards = 1_000))

        composeRule.onNode(hasProgressRange).assert(readsContaining("1,512장 중 1,000장 저장됨")).assert(isPoliteRegion)
    }

    @Test
    fun `보통 상태와 다시 시도하는 중에는 가져오기 멈추기 버튼이 활성이다`() {
        host.show(progress(status = SavingStatus.Saving))
        composeRule.onNodeWithText(STOP).assertIsEnabled()

        host.update(progress(status = SavingStatus.Retrying))
        composeRule.onNodeWithText(STOP).assertIsEnabled()
    }

    @Test
    fun `가져오기 멈추기 버튼을 누르면 멈추기를 한 번 요청한다`() {
        host.show()

        composeRule.onNodeWithText(STOP).performClick()

        host.stopClicks shouldBe 1
    }

    @Test
    fun `멈추는 중에는 가져오기 멈추기 버튼이 꺼져 눌러도 요청하지 않는다`() {
        host.show(progress(status = SavingStatus.Stopping))

        composeRule.onNodeWithText(STOP).assertIsNotEnabled().performClick()

        host.stopClicks shouldBe 0
    }
}
