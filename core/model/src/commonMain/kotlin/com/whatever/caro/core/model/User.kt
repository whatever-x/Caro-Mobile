package com.whatever.caro.core.model

import androidx.compose.runtime.Stable

@Stable
data class User(
    val id: Long,
    val name: String,
)
