package com.whatever.caro.core.remote.dto.auth.request

import com.whatever.caro.core.model.auth.SocialLoginType
import kotlinx.serialization.Serializable

@Serializable
data class SocialLoginRequest(
    val provider: SocialLoginType,
    val idToken: String,
)
