package com.whatever.caro.core.remote.dto.user.request

import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class UpdateNicknameRequest(
    /** 변경할 닉네임 */
    val nickname: String,
)
