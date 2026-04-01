package com.whatever.caro.core.remote.model.profile.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateProfileRequest(
    val nickname: String,
)
