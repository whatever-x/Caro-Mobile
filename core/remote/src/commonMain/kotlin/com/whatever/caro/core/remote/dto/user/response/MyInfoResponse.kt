package com.whatever.caro.core.remote.dto.user.response

import com.whatever.caro.core.model.auth.SocialLoginType
import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class MyInfoResponse(
    /** 닉네임 */
    val nickname: String?,
    /** 이메일. 소셜 제공자가 미제공한 경우 null */
    val email: String?,
    /** 유저가 가입한 로그인 플랫폼 */
    val loginPlatform: SocialLoginType?,
)
