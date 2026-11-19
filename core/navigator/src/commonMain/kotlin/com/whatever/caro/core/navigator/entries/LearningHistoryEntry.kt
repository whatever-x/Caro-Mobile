package com.whatever.caro.core.navigator.entries

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 리포트에서 고른 덱과 기간의 학습 기록. deckId가 null이면 전체, 날짜가 null이면 전체 기간이다.
 * 날짜는 yyyy-MM-dd다.
 */
@Serializable
data class LearningHistoryEntry(
    val deckId: Long?,
    val deckName: String?,
    val startDate: String?,
    val endDate: String?,
) : NavKey
