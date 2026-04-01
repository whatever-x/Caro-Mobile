package com.whatever.caro.core.remote.model.profile.response

import kotlinx.serialization.Serializable

@Serializable
data class CreateProfileResponse(
    val userId: Long,
    val nickname: String,
)
