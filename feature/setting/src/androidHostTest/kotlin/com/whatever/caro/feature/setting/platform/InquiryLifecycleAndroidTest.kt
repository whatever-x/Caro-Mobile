package com.whatever.caro.feature.setting.platform

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE)
class InquiryLifecycleAndroidTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `navigation entry STOP is not application background`() {
        val entry = EntryLifecycleOwner()
        var backgrounds = 0
        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides entry) {
                InquiryLifecycleEffect(onBackground = { backgrounds++ }, onForeground = {})
            }
        }

        composeRule.runOnIdle { entry.registry.currentState = Lifecycle.State.CREATED }

        composeRule.runOnIdle { backgrounds shouldBe 0 }
    }

    @Test
    fun `host Activity STOP and RESUME are observed independently of navigation entry`() {
        val entry = EntryLifecycleOwner()
        var backgrounds = 0
        var foregrounds = 0
        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides entry) {
                InquiryLifecycleEffect(onBackground = { backgrounds++ }, onForeground = { foregrounds++ })
            }
        }
        composeRule.runOnIdle { foregrounds = 0 }

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        backgrounds shouldBe 1
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.runOnIdle { foregrounds shouldBe 1 }
    }

    @Test
    fun `configuration recreation is not application background`() {
        var backgrounds = 0
        composeRule.setContent {
            InquiryLifecycleEffect(onBackground = { backgrounds++ }, onForeground = {})
        }

        composeRule.activityRule.scenario.recreate()

        backgrounds shouldBe 0
    }

    @Test
    fun `recomposition uses the latest background callback`() {
        val version = mutableStateOf("old")
        val events = mutableListOf<String>()
        composeRule.setContent {
            val currentVersion = version.value
            InquiryLifecycleEffect(onBackground = { events += currentVersion }, onForeground = {})
        }
        composeRule.runOnIdle { version.value = "new" }
        composeRule.waitForIdle()

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)

        events shouldBe listOf("new")
    }

    @Test
    fun `removed effect no longer receives host callbacks`() {
        val visible = mutableStateOf(true)
        var backgrounds = 0
        composeRule.setContent {
            if (visible.value) InquiryLifecycleEffect(onBackground = { backgrounds++ }, onForeground = {})
        }
        composeRule.runOnIdle { visible.value = false }
        composeRule.waitForIdle()

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)

        backgrounds shouldBe 0
    }
}

private class EntryLifecycleOwner : LifecycleOwner {
    val registry = LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
    override val lifecycle: Lifecycle = registry
}
