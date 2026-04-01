package com.whatever.caro.core.remote.model.profile.response

import kotlinx.serialization.Serializable

@Serializable
data class ValidateNicknameResponse(
    val isValid: Boolean,
    val reason: String? = null,
)
