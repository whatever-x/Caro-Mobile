package com.whatever.caro.core.model.auth

import kotlinx.serialization.Serializable

@Serializable
enum class SocialLoginType {
    GOOGLE,
    APPLE,
    NONE,
}
