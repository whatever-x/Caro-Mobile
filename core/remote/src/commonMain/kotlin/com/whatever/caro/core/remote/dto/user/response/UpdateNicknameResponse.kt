package com.whatever.caro.core.remote.dto.user.response

import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class UpdateNicknameResponse(
    /** 사용자 ID */
    val userId: Long?,
    /** 변경된 닉네임 */
    val nickname: String?,
)
