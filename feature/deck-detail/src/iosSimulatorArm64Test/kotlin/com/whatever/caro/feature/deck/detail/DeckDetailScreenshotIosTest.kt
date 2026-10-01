package com.whatever.caro.feature.deck.detail

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import io.github.takahirom.roborazzi.captureRoboImage
import platform.Foundation.NSUserDefaults
import kotlin.test.Test

class DeckDetailScreenshotIosTest {
    @OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
    @Test
    fun initial() {
        val defaults = NSUserDefaults.standardUserDefaults
        val previousLanguages = defaults.objectForKey("AppleLanguages")
        defaults.setObject(listOf(DeckDetailScreenshotScenario.LOCALE), forKey = "AppleLanguages")

        try {
            runSkikoComposeUiTest(
                size = Size(DeckDetailScreenshotScenario.WIDTH_DP.toFloat(), DeckDetailScreenshotScenario.HEIGHT_DP.toFloat()),
            ) {
                setContent { DeckDetailScreenshotScenario.Render() }
                waitForIdle()
                onRoot().captureRoboImage(this, filePath = DeckDetailScreenshotScenario.FILE_NAME)
            }
        } finally {
            if (previousLanguages == null) {
                defaults.removeObjectForKey("AppleLanguages")
            } else {
                defaults.setObject(previousLanguages, forKey = "AppleLanguages")
            }
        }
    }

    @OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
    @Test
    fun libraryCopy() {
        val defaults = NSUserDefaults.standardUserDefaults
        val previousLanguages = defaults.objectForKey("AppleLanguages")
        defaults.setObject(listOf(DeckDetailScreenshotScenario.LOCALE), forKey = "AppleLanguages")

        try {
            runSkikoComposeUiTest(
                size = Size(DeckDetailScreenshotScenario.WIDTH_DP.toFloat(), DeckDetailScreenshotScenario.HEIGHT_DP.toFloat()),
            ) {
                setContent { DeckDetailScreenshotScenario.LibraryCopy() }
                waitForIdle()
                onRoot().captureRoboImage(this, filePath = "deck_detail.library_copy.png")
            }
        } finally {
            if (previousLanguages == null) {
                defaults.removeObjectForKey("AppleLanguages")
            } else {
                defaults.setObject(previousLanguages, forKey = "AppleLanguages")
            }
        }
    }
}
