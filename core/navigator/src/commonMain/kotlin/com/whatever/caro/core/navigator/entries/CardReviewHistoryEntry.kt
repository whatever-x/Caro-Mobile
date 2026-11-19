package com.whatever.caro.core.navigator.entries

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 덱 리포트에서 고른 카드의 평가 이력. 날짜가 null이면 전체 기간이고 yyyy-MM-dd다.
 */
@Serializable
data class CardReviewHistoryEntry(
    val deckId: Long,
    val cardId: Long,
    val startDate: String?,
    val endDate: String?,
) : NavKey
