package com.whatever.caro.feature.home.library

import com.whatever.caro.core.data.repository.library.LibraryRepository
import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.home.library.mvi.LibraryPreviewIntent
import com.whatever.caro.feature.home.library.mvi.LibraryPreviewState
import com.whatever.caro.feature.home.library.mvi.LibrarySideEffect
import com.whatever.caro.feature.home.library.mvi.libraryFailure
import com.whatever.caro.feature.home.library.mvi.toUiContent
import kotlin.uuid.Uuid

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class LibraryPreviewViewModel(
    private val libraryDeckId: Long,
    private val repository: LibraryRepository,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<LibraryPreviewState, LibraryPreviewIntent, LibrarySideEffect>(LibraryPreviewState(), exceptionFilter) {
    private var generation = 0L
    private var requestKey = Uuid.random().toString()

    override suspend fun handleIntent(intent: LibraryPreviewIntent) {
        when (intent) {
            LibraryPreviewIntent.Initialize -> if (currentState.detail == null) load()
            LibraryPreviewIntent.Retry -> load()
            LibraryPreviewIntent.Back -> postSideEffect(LibrarySideEffect.Back)
            LibraryPreviewIntent.Login -> postSideEffect(LibrarySideEffect.Login)
            LibraryPreviewIntent.Home -> postSideEffect(LibrarySideEffect.Home)
            LibraryPreviewIntent.Add -> add()
            LibraryPreviewIntent.DismissError -> reduce { copy(showAddError = false) }
        }
    }

    private suspend fun load() {
        val request = ++generation
        reduce { copy(loading = true, failure = null) }
        val result = suspendRunCatching { repository.detail(libraryDeckId) }
        if (request != generation) return
        result.fold(
            onSuccess = { reduce { copy(detail = it.toUiContent(), loading = false) } },
            onFailure = { error -> reduce { copy(loading = false, failure = error.libraryFailure()) } },
        )
    }

    private suspend fun add() {
        if (currentState.adding || currentState.showAddError || currentState.detail == null) return
        reduce { copy(adding = true) }
        suspendRunCatching { repository.copy(libraryDeckId, requestKey) }.fold(
            onSuccess = {
                requestKey = Uuid.random().toString()
                reduce { copy(adding = false) }
                postSideEffect(LibrarySideEffect.PersonalDeck(it))
            },
            onFailure = { reduce { copy(adding = false, showAddError = true) } },
        )
    }
}
