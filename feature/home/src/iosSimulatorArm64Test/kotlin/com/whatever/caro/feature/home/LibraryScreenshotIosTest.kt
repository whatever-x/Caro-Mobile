package com.whatever.caro.feature.home

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.whatever.caro.feature.home.library.mvi.LibraryPreviewIntent
import io.github.takahirom.roborazzi.captureRoboImage
import platform.Foundation.NSUserDefaults
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryScreenshotIosTest {
    @OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
    @Test
    fun englishPreviewOnNarrowScreen() {
        val defaults = NSUserDefaults.standardUserDefaults
        val previousLanguages = defaults.objectForKey("AppleLanguages")
        defaults.setObject(listOf("en"), forKey = "AppleLanguages")
        try {
            runSkikoComposeUiTest(size = Size(320f, 700f)) {
                setContent { LibraryScreenshotScenario.EnglishPreview() }
                waitForIdle()
                onNodeWithText("Add to my decks").assertIsDisplayed()
                onNodeWithText("Cards: 1").assertIsDisplayed()
                onRoot().captureRoboImage(this, filePath = "library.preview.english.narrow.png")
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
    fun englishErrorOnlyClosesPopup() {
        val defaults = NSUserDefaults.standardUserDefaults
        val previousLanguages = defaults.objectForKey("AppleLanguages")
        defaults.setObject(listOf("en"), forKey = "AppleLanguages")
        try {
            val intents = mutableListOf<LibraryPreviewIntent>()
            runSkikoComposeUiTest(size = Size(320f, 700f)) {
                setContent { LibraryScreenshotScenario.EnglishError { intents.add(it) } }
                waitForIdle()
                onNodeWithText("Couldn't add deck").assertIsDisplayed()
                onNode(isRoot() and hasAnyDescendant(hasText("Couldn't add deck")))
                    .captureRoboImage(this, filePath = "library.preview.english.error.png")
                onNodeWithText("OK").performClick()
                assertEquals(listOf<LibraryPreviewIntent>(LibraryPreviewIntent.DismissError), intents)
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
    fun preview() {
        val defaults = NSUserDefaults.standardUserDefaults
        val previousLanguages = defaults.objectForKey("AppleLanguages")
        defaults.setObject(listOf(HomeScreenshotScenario.LOCALE), forKey = "AppleLanguages")

        try {
            runSkikoComposeUiTest(
                size = Size(402f, 790f),
            ) {
                setContent { LibraryScreenshotScenario.Preview() }
                waitForIdle()
                onRoot().captureRoboImage(this, filePath = "library.preview.normal.png")
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
    fun list() {
        val defaults = NSUserDefaults.standardUserDefaults
        val previousLanguages = defaults.objectForKey("AppleLanguages")
        defaults.setObject(listOf(HomeScreenshotScenario.LOCALE), forKey = "AppleLanguages")

        try {
            runSkikoComposeUiTest(
                size = Size(402f, 790f),
            ) {
                setContent { LibraryScreenshotScenario.List() }
                waitForIdle()
                onRoot().captureRoboImage(this, filePath = "library.list.normal.png")
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
    fun narrow() {
        val defaults = NSUserDefaults.standardUserDefaults
        val previousLanguages = defaults.objectForKey("AppleLanguages")
        defaults.setObject(listOf(HomeScreenshotScenario.LOCALE), forKey = "AppleLanguages")

        try {
            runSkikoComposeUiTest(
                size = Size(320f, 700f),
            ) {
                setContent { LibraryScreenshotScenario.Preview(narrow = true) }
                waitForIdle()
                onRoot().captureRoboImage(this, filePath = "library.preview.narrow.png")
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
