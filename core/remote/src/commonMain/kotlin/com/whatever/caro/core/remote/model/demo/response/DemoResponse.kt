package com.whatever.caro.core.remote.model.demo.response

import com.whatever.caro.core.remote.model.DemoDto
import kotlinx.serialization.Serializable

@Serializable
data class DemoResponse(
    val user: DemoDto,
)
