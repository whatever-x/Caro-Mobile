package com.whatever.caro.core.remote.model.profile.response

import kotlinx.serialization.Serializable

@Serializable
data class NicknameAvailabilityResponse(
    val available: Boolean,
)
