package com.whatever.caro.feature.learning.report.history.mvi

import com.whatever.caro.core.model.report.ReportPeriod
import com.whatever.caro.core.model.report.ReportSession
import com.whatever.caro.core.viewmodel.contract.UiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class LearningHistoryState(
    val deckId: Long?,
    val deckName: String?,
    val period: ReportPeriod?,
    val sessions: ImmutableList<ReportSession> = persistentListOf(),
    val cursor: String? = null,
    val hasLoaded: Boolean = false,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
) : UiState {
    val hasMore: Boolean
        get() = hasLoaded.not() || cursor != null
}
