package com.whatever.caro.feature.deck.detail

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.feature.deck.detail.model.CardItem
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class DeckDetailScreenshotAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initial() {
        composeRule.setContent { DeckDetailScreenshotScenario.Render() }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(DeckDetailScreenshotScenario.FILE_NAME)
    }

    @Test
    fun libraryCopy() {
        composeRule.setContent {
            CaroTheme {
                DeckDetailScreen(
                    DeckDetailState(
                        deck = Deck(1, "토익 출제 단어 200개 (2)", "토익 준비를 위한 단어와 뜻", 200, 0, 0, DeckState.NOT_STARTED),
                        deckCardList =
                            persistentListOf(
                                CardItem(1, "apply", "지원하다"),
                                CardItem(2, "confirm", "확인하다"),
                                CardItem(3, "schedule", "일정"),
                                CardItem(4, "invoice", "청구서"),
                            ),
                    ),
                    onIntent = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("deck_detail.library_copy.png")
    }
}
