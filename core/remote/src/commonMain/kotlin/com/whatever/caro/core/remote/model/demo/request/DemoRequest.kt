package com.whatever.caro.core.remote.model.demo.request

import kotlinx.serialization.Serializable

@Serializable
data class DemoRequest(
    val id: Long,
)
