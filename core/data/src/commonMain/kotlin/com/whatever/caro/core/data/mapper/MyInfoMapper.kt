package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.model.profile.MyInfo
import com.whatever.caro.core.remote.dto.user.response.MyInfoResponse

internal fun MyInfoResponse.toMyInfo(): MyInfo =
    MyInfo(
        nickname = nickname.orEmpty(),
        email = email.orEmpty(),
        socialLoginType = loginPlatform ?: SocialLoginType.NONE,
    )

internal fun MyInfoResponse.toMyNickname(): String = nickname.orEmpty()
