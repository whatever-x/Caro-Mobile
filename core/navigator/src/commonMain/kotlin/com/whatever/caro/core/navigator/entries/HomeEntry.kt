package com.whatever.caro.core.navigator.entries

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class HomeEntry(
    val payload: Payload,
): NavKey

@Serializable
data class Payload(
    val id: Int,
    val name: String
)