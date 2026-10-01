package com.whatever.caro.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.home.library.LibraryPreviewScreen
import com.whatever.caro.feature.home.library.mvi.LibraryPreviewIntent
import com.whatever.caro.feature.home.library.mvi.LibraryPreviewState
import com.whatever.caro.feature.home.library.mvi.toUiContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w402dp-h790dp-xxhdpi")
class LibraryScreenAndroidTest {
    @get:Rule val composeRule = createComposeRule()
    private val detail = LibraryScreenshotScenario.detail

    @Test fun preview() {
        composeRule.setContent { LibraryScreenshotScenario.Preview() }
        composeRule.onNodeWithText("내 덱에 추가").assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("library.preview.normal.png")
    }

    @Test fun list() {
        composeRule.setContent { LibraryScreenshotScenario.List() }
        composeRule.onRoot().captureRoboImage("library.list.normal.png")
    }

    @Test
    @Config(qualifiers = "ko-rKR-w320dp-h700dp-xxhdpi")
    fun narrowLongTitle() {
        composeRule.setContent { LibraryScreenshotScenario.Preview(narrow = true) }
        composeRule.onNodeWithText("내 덱에 추가").assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("library.preview.narrow.png")
    }

    @Test fun addingPreventsClick() {
        composeRule.setContent {
            CaroTheme {
                LibraryPreviewScreen(
                    LibraryPreviewState(detail.toUiContent(), loading = false, adding = true),
                ) {}
            }
        }
        composeRule.onNodeWithText("추가 중…").assertIsNotEnabled()
    }

    @Test
    @Config(qualifiers = "ko-rKR-w320dp-h700dp-xxhdpi")
    fun errorConfirmOnlyClosesPopup() {
        val intents = mutableListOf<LibraryPreviewIntent>()
        composeRule.setContent {
            CaroTheme {
                LibraryPreviewScreen(
                    LibraryPreviewState(detail.toUiContent(), loading = false, showAddError = true),
                ) { intents.add(it) }
            }
        }
        composeRule.onNodeWithText("덱 추가 중 오류가 발생했어요").assertIsDisplayed()
        composeRule.onNodeWithText("확인").performClick()
        assertEquals(listOf(LibraryPreviewIntent.DismissError), intents)
    }
}
