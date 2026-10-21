package com.whatever.caro.feature.learning.report.card.mvi

import com.whatever.caro.core.model.report.ReportCardLabel
import com.whatever.caro.core.model.report.ReportPeriod
import com.whatever.caro.core.model.report.ReportReview
import com.whatever.caro.core.viewmodel.contract.UiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CardReviewHistoryState(
    val period: ReportPeriod?,
    val card: ReportCardLabel? = null,
    val reviews: ImmutableList<ReportReview> = persistentListOf(),
    val cursor: String? = null,
    val hasLoaded: Boolean = false,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
) : UiState {
    val hasMore: Boolean
        get() = hasLoaded.not() || cursor != null
}
