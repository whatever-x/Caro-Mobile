package com.whatever.caro.core.navigator.entries

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * deckId가 null이면 전체 리포트, 있으면 그 덱의 리포트로 연다.
 */
@Serializable
data class LearningReportEntry(
    val deckId: Long? = null,
) : NavKey
