package com.whatever.caro.core.navigator.entries

import androidx.navigation3.runtime.NavKey
import com.whatever.caro.core.model.learning.LearningMode
import kotlinx.serialization.Serializable

@Serializable
data class LearningEntry(
    val deckId: Long,
    val mode : LearningMode,
) : NavKey
