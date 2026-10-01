package com.whatever.caro.feature.home

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.library.LibraryCard
import com.whatever.caro.core.model.library.LibraryDeck
import com.whatever.caro.core.model.library.LibraryDetail
import com.whatever.caro.feature.home.library.LibraryPreviewScreen
import com.whatever.caro.feature.home.library.LibraryScreen
import com.whatever.caro.feature.home.library.mvi.LibraryPreviewState
import com.whatever.caro.feature.home.library.mvi.LibraryState
import com.whatever.caro.feature.home.library.mvi.toUiContent
import kotlinx.collections.immutable.persistentListOf

internal object LibraryScreenshotScenario {
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

    @Composable
    fun Preview(narrow: Boolean = false) {
        val content = if (narrow) detail.copy(deck = deck.copy(name = "아주 긴 제목의 단어 학습용 제공 덱 이름을 표시하는 예시", description = "")) else detail
        CaroTheme { LibraryPreviewScreen(LibraryPreviewState(content.toUiContent(), loading = false)) {} }
    }

    @Composable
    fun List() {
        CaroTheme { LibraryScreen(LibraryState(persistentListOf(deck), loading = false, loaded = true)) {} }
    }
}
