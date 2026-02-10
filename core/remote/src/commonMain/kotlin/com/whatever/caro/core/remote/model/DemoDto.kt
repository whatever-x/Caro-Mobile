package com.whatever.caro.core.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class DemoDto(
    val id: Long,
    val name: String,
)
