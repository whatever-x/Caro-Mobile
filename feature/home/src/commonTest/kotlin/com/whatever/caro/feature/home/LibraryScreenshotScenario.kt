package com.whatever.caro.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

internal object LibraryScreenshotScenario {
    const val PREVIEW_ID = "library.preview.normal"
    const val LIST_ID = "library.list.normal"
    const val NARROW_ID = "library.preview.narrow"
    const val ENGLISH_NARROW_ID = "library.preview.english.narrow"
    const val ENGLISH_ERROR_ID = "library.preview.english.error"

    fun scenarios(): List<ScreenshotScenario> {
        var adding by mutableStateOf(false)
        val intents = mutableListOf<LibraryPreviewIntent>()
        return listOf(
            ScreenshotScenario(
                PREVIEW_ID,
                render = { Preview(adding = adding) },
                beforeCapture = { language ->
                    onNodeWithText(if (language == "ko") "내 덱에 추가" else "Add to my decks").assertIsDisplayed()
                },
                afterCapture = { language ->
                    adding = true
                    onNodeWithText(if (language == "ko") "추가 중…" else "Adding…").assertIsNotEnabled()
                },
            ),
            ScreenshotScenario(LIST_ID, render = { List() }),
            ScreenshotScenario(
                NARROW_ID,
                render = { Preview(narrow = true) },
                beforeCapture = { language ->
                    onNodeWithText(if (language == "ko") "내 덱에 추가" else "Add to my decks").assertIsDisplayed()
                },
            ),
            ScreenshotScenario(
                ENGLISH_NARROW_ID,
                render = { EnglishPreview() },
                beforeCapture = { language ->
                    onNodeWithText(if (language == "ko") "내 덱에 추가" else "Add to my decks").assertIsDisplayed()
                    onNodeWithText(if (language == "ko") "1개 카드" else "Cards: 1").assertIsDisplayed()
                },
            ),
            ScreenshotScenario(
                ENGLISH_ERROR_ID,
                render = { EnglishError { intents.add(it) } },
                isDialog = true,
                beforeCapture = { language ->
                    onNodeWithText(if (language == "ko") "덱 추가 중 오류가 발생했어요" else "Couldn't add deck").assertIsDisplayed()
                },
                afterCapture = { language ->
                    onNodeWithText(if (language == "ko") "확인" else "OK").performClick()
                    check(intents == listOf(LibraryPreviewIntent.DismissError)) { "Error confirmation must only dismiss the popup" }
                },
            ),
        )
    }

    private val deck = LibraryDeck(1, "토익 출제 단어 200개", "토익 준비를 위한 단어와 뜻", 200)
    val detail =
        LibraryDetail(
            deck,
            listOf(
                LibraryCard(1, "apply", "지원하다", 0),
                LibraryCard(2, "confirm", "확인하다", 1),
                LibraryCard(3, "schedule", "일정", 2),
                LibraryCard(4, "invoice", "청구서", 3),
            ),
        )

    private val englishDetail =
        detail.copy(
            deck =
                deck.copy(
                    name = "Essential English vocabulary for work and travel",
                    description = "Everyday words for work, travel, and daily life.",
                    cardCount = 1,
                ),
            cards = detail.cards.take(1),
        )

    @Composable
    fun Preview(
        narrow: Boolean = false,
        adding: Boolean = false,
    ) {
        val content = if (narrow) detail.copy(deck = deck.copy(name = "아주 긴 제목의 단어 학습용 제공 덱 이름을 표시하는 예시", description = "")) else detail
        CaroTheme { LibraryPreviewScreen(LibraryPreviewState(content.toUiContent(), loading = false, adding = adding)) {} }
    }

    @Composable
    fun List() {
        CaroTheme { LibraryScreen(LibraryState(persistentListOf(deck), loading = false, loaded = true)) {} }
    }

    @Composable
    fun EnglishPreview() {
        CaroTheme { LibraryPreviewScreen(LibraryPreviewState(englishDetail.toUiContent(), loading = false)) {} }
    }

    @Composable
    fun EnglishError(onIntent: (LibraryPreviewIntent) -> Unit) {
        CaroTheme {
            LibraryPreviewScreen(
                LibraryPreviewState(englishDetail.toUiContent(), loading = false, showAddError = true),
                onIntent,
            )
        }
    }
}
