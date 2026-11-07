package com.whatever.caro.feature.home.library.mvi

import androidx.compose.runtime.Immutable
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.exception.CaroAuthException
import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.core.model.library.LibraryCard
import com.whatever.caro.core.model.library.LibraryDeck
import com.whatever.caro.core.model.library.LibraryDetail
import com.whatever.caro.core.viewmodel.contract.UiIntent
import com.whatever.caro.core.viewmodel.contract.UiSideEffect
import com.whatever.caro.core.viewmodel.contract.UiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

enum class LibraryFailure { NETWORK, LOGIN, FORBIDDEN, UNAVAILABLE }

internal fun Throwable.libraryFailure(): LibraryFailure =
    if (this is CaroAuthException) {
        LibraryFailure.LOGIN
    } else {
        when ((this as? CaroServerException)?.code) {
            "L001" -> LibraryFailure.UNAVAILABLE
            "A001", "A003", "A004", "A005", "A006" -> LibraryFailure.LOGIN
            "A002" -> LibraryFailure.FORBIDDEN
            else -> LibraryFailure.NETWORK
        }
    }

data class LibraryState(
    val decks: ImmutableList<LibraryDeck> = persistentListOf(),
    val loading: Boolean = true,
    val loaded: Boolean = false,
    val failure: LibraryFailure? = null,
) : UiState

@Immutable
data class LibraryPreviewContent(
    val deck: LibraryDeck,
    val cards: ImmutableList<LibraryCard>,
)

internal fun LibraryDetail.toUiContent() = LibraryPreviewContent(deck, cards.toImmutableList())

data class LibraryPreviewState(
    val detail: LibraryPreviewContent? = null,
    val loading: Boolean = true,
    val failure: LibraryFailure? = null,
    val adding: Boolean = false,
    val showAddError: Boolean = false,
) : UiState

sealed interface LibraryIntent : UiIntent {
    data object Initialize : LibraryIntent

    data object Retry : LibraryIntent

    data object Back : LibraryIntent

    data object Login : LibraryIntent

    data object Home : LibraryIntent

    data class Open(
        val id: Long,
    ) : LibraryIntent
}

sealed interface LibraryPreviewIntent : UiIntent {
    data object Initialize : LibraryPreviewIntent

    data object Retry : LibraryPreviewIntent

    data object Back : LibraryPreviewIntent

    data object Login : LibraryPreviewIntent

    data object Home : LibraryPreviewIntent

    data object Add : LibraryPreviewIntent

    data object DismissError : LibraryPreviewIntent
}

sealed interface LibrarySideEffect : UiSideEffect {
    data object Back : LibrarySideEffect

    data object Login : LibrarySideEffect

    data object Home : LibrarySideEffect

    data class Preview(
        val id: Long,
    ) : LibrarySideEffect

    data class PersonalDeck(
        val deck: Deck,
    ) : LibrarySideEffect
}
