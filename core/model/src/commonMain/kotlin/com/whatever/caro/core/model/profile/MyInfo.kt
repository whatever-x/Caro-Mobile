package com.whatever.caro.core.model.profile

import com.whatever.caro.core.model.auth.SocialLoginType

data class MyInfo(
    val nickname: String,
    val email: String,
    val socialLoginType: SocialLoginType,
)
