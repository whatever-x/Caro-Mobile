package com.whatever.caro.feature.home.library

import com.whatever.caro.core.data.repository.library.LibraryRepository
import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.home.library.mvi.LibraryIntent
import com.whatever.caro.feature.home.library.mvi.LibrarySideEffect
import com.whatever.caro.feature.home.library.mvi.LibraryState
import com.whatever.caro.feature.home.library.mvi.libraryFailure
import kotlinx.collections.immutable.toImmutableList

class LibraryViewModel(
    private val repository: LibraryRepository,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<LibraryState, LibraryIntent, LibrarySideEffect>(LibraryState(), exceptionFilter) {
    private var generation = 0L

    override suspend fun handleIntent(intent: LibraryIntent) {
        when (intent) {
            LibraryIntent.Initialize -> load()
            LibraryIntent.Retry -> load()
            LibraryIntent.Back -> postSideEffect(LibrarySideEffect.Back)
            LibraryIntent.Login -> postSideEffect(LibrarySideEffect.Login)
            LibraryIntent.Home -> postSideEffect(LibrarySideEffect.Home)
            is LibraryIntent.Open -> postSideEffect(LibrarySideEffect.Preview(intent.id))
        }
    }

    private suspend fun load() {
        val request = ++generation
        reduce { copy(loading = true, failure = null) }
        val result = suspendRunCatching { repository.list().toImmutableList() }
        if (request != generation) return
        result.fold(
            onSuccess = { reduce { copy(decks = it, loaded = true, loading = false) } },
            onFailure = { error -> reduce { copy(loading = false, failure = error.libraryFailure()) } },
        )
    }
}
