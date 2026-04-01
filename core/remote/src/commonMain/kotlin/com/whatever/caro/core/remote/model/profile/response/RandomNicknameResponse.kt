package com.whatever.caro.core.remote.model.profile.response

import kotlinx.serialization.Serializable

@Serializable
data class RandomNicknameResponse(
    val nickname: String,
)
