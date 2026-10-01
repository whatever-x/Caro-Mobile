package com.whatever.caro.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.library.LibraryCard
import com.whatever.caro.core.model.library.LibraryDeck
import com.whatever.caro.core.model.library.LibraryDetail
import com.whatever.caro.feature.home.library.LibraryPreviewScreen
import com.whatever.caro.feature.home.library.LibraryScreen
import com.whatever.caro.feature.home.library.mvi.LibraryPreviewIntent
import com.whatever.caro.feature.home.library.mvi.LibraryPreviewState
import com.whatever.caro.feature.home.library.mvi.LibraryState
import com.whatever.caro.feature.home.library.mvi.toUiContent
import kotlinx.collections.immutable.persistentListOf
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
    private val deck = LibraryDeck(1, "토익 출제 단어 200개", "토익 준비를 위한 단어와 뜻", 200)
    private val detail =
        LibraryDetail(
            deck,
            listOf(
                LibraryCard(1, "apply", "지원하다", 0),
                LibraryCard(2, "confirm", "확인하다", 1),
                LibraryCard(3, "schedule", "일정", 2),
                LibraryCard(4, "invoice", "청구서", 3),
            ),
        )

    @Test fun preview() {
        composeRule.setContent { CaroTheme { LibraryPreviewScreen(LibraryPreviewState(detail.toUiContent(), loading = false)) {} } }
        composeRule.onNodeWithText("내 덱에 추가").assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("library.preview.normal.png")
    }

    @Test fun list() {
        composeRule.setContent { CaroTheme { LibraryScreen(LibraryState(persistentListOf(deck), loading = false, loaded = true)) {} } }
        composeRule.onRoot().captureRoboImage("library.list.normal.png")
    }

    @Test
    @Config(qualifiers = "ko-rKR-w320dp-h700dp-xxhdpi")
    fun narrowLongTitle() {
        composeRule.setContent {
            CaroTheme {
                LibraryPreviewScreen(
                    LibraryPreviewState(
                        detail.copy(deck = deck.copy(name = "아주 긴 제목의 단어 학습용 제공 덱 이름을 표시하는 예시", description = "")).toUiContent(),
                        loading = false,
                    ),
                ) {
                }
            }
        }
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
