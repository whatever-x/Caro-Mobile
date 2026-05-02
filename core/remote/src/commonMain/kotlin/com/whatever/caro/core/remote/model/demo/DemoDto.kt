package com.whatever.caro.core.remote.model.demo

import kotlinx.serialization.Serializable

@Serializable
data class DemoDto(
    val id: Long,
    val name: String,
)
