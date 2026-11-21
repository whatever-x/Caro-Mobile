package com.whatever.caro.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_title_create
import caromobile.core.designsystem.generated.resources.home_create_deck_sheet_import_title
import caromobile.core.designsystem.generated.resources.home_create_deck_sheet_manual_title
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.screenshot.testing.ScreenshotScenario
import com.whatever.caro.core.screenshot.testing.ScreenshotString
import com.whatever.caro.feature.home.component.CreateDeckSheetContainerColor
import com.whatever.caro.feature.home.component.CreateDeckSheetContent
import com.whatever.caro.feature.home.component.CreateDeckSheetHandle
import com.whatever.caro.feature.home.component.CreateDeckSheetShape
import com.whatever.caro.feature.home.mvi.HomeState
import com.whatever.caro.feature.home.mvi.HomeStreakState
import kotlinx.collections.immutable.persistentListOf

internal object HomeScreenshotScenario {
    const val ID = "home.home.with_deck"
    const val CREATE_SHEET_ID = "home.home.create_sheet"

    private val withDeckState =
        HomeState(
            nickname = "승우",
            streakState = HomeStreakState.NotStarted,
            decks = persistentListOf(Deck(1, "Android", "기초 학습", 10, 5, 0, DeckState.NOT_STARTED)),
            isLoading = false,
        )

    fun scenarios() =
        listOf(
            ScreenshotScenario(
                ID,
                render = { Render() },
                expectedStrings = listOf(ScreenshotString(Res.string.deck_title_create)),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.deck_title_create]).assertIsDisplayed()
                },
            ),
            ScreenshotScenario(
                CREATE_SHEET_ID,
                render = { RenderCreateSheet() },
                expectedStrings =
                    listOf(
                        ScreenshotString(Res.string.home_create_deck_sheet_manual_title),
                        ScreenshotString(Res.string.home_create_deck_sheet_import_title),
                    ),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.home_create_deck_sheet_manual_title]).assertIsDisplayed()
                    onNodeWithText(strings[Res.string.home_create_deck_sheet_import_title]).assertIsDisplayed()
                },
            ),
        )

    @Composable
    fun Render() {
        CaroTheme {
            HomeScreen(
                state = withDeckState,
                onIntent = {
                },
            )
        }
    }

    // ModalBottomSheet는 별도 창이라 화면 캡처의 root가 둘이 되므로, 시트의 손잡이와 본문을 홈 위 스크림의 아래쪽에 직접 그린다.
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun RenderCreateSheet() {
        CaroTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                HomeScreen(state = withDeckState, onIntent = {})
                Box(modifier = Modifier.matchParentSize().background(BottomSheetDefaults.ScrimColor))
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .clip(CreateDeckSheetShape)
                            .background(CreateDeckSheetContainerColor),
                ) {
                    CreateDeckSheetHandle()
                    CreateDeckSheetContent(onCreateDeck = {}, onImportDeck = {})
                }
            }
        }
    }
}
