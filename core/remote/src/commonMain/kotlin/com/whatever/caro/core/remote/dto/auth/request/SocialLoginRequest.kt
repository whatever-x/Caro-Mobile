package com.whatever.caro.core.remote.dto.auth.request

import com.whatever.caro.core.model.auth.SocialLoginType
import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class SocialLoginRequest(
    val provider: SocialLoginType,
    val idToken: String,
)
