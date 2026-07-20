package com.whatever.caro.core.navigator.entries

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class EditCardEntry(
    val payload: Payload,
) : NavKey {
    @Serializable
    data class Payload(
        val cardId: Long,
        val front: String,
        val back: String,
    )
}
