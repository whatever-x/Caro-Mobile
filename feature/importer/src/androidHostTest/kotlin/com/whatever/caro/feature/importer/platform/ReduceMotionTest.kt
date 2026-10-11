package com.whatever.caro.feature.importer.platform

import android.provider.Settings
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// 개발자 옵션의 애니메이션 배율이 0이면 시스템 동작 줄이기가 켜진 것으로 본다. 값이 아예 없으면 기본 배율 1이다.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class ReduceMotionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun putScale(
        name: String,
        value: Float,
    ) {
        Settings.Global.putFloat(RuntimeEnvironment.getApplication().contentResolver, name, value)
    }

    private fun readReduceMotion(): Boolean {
        var result: Boolean? = null
        composeRule.setContent { result = rememberReduceMotionEnabled() }
        composeRule.waitForIdle()
        return checkNotNull(result)
    }

    @Test
    fun `설정을 바꾸지 않았으면 동작 줄이기가 꺼져 있다`() {
        readReduceMotion() shouldBe false
    }

    @Test
    fun `애니메이터 배율이 0이면 동작 줄이기가 켜져 있다`() {
        putScale(Settings.Global.ANIMATOR_DURATION_SCALE, 0f)

        readReduceMotion() shouldBe true
    }

    @Test
    fun `화면 전환 배율이 0이면 동작 줄이기가 켜져 있다`() {
        putScale(Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)

        readReduceMotion() shouldBe true
    }

    @Test
    fun `두 배율이 모두 기본값 1이면 동작 줄이기가 꺼져 있다`() {
        putScale(Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        putScale(Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)

        readReduceMotion() shouldBe false
    }

    @Test
    fun `배율이 0이 아닌 느린 값이면 동작 줄이기가 꺼져 있다`() {
        putScale(Settings.Global.ANIMATOR_DURATION_SCALE, 0.5f)
        putScale(Settings.Global.TRANSITION_ANIMATION_SCALE, 0.5f)

        readReduceMotion() shouldBe false
    }

    @Test
    fun `애니메이터 배율만 0이어도 화면 전환 배율이 1이면 켜져 있다`() {
        putScale(Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        putScale(Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)

        readReduceMotion() shouldBe true
    }
}
