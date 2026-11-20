package com.whatever.caro.feature.importer.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LargeTextTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun isLargeTextAt(fontScale: Float): Boolean {
        var result: Boolean? = null
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 3f, fontScale = fontScale)) {
                result = isLargeText()
            }
        }
        composeRule.waitForIdle()
        return checkNotNull(result)
    }

    @Test
    fun `글자 크기가 130퍼센트이면 큰 글자로 본다`() {
        isLargeTextAt(fontScale = 1.3f) shouldBe true
    }

    @Test
    fun `글자 크기가 130퍼센트보다 작으면 큰 글자로 보지 않는다`() {
        isLargeTextAt(fontScale = 1.29f) shouldBe false
    }
}
