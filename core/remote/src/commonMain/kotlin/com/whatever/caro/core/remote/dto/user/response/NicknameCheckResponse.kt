package com.whatever.caro.core.remote.dto.user.response

import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class NicknameCheckResponse(
    /** 조회 대상 닉네임 */
    val nickname: String?,
    /** 사용 가능 여부 */
    val available: Boolean,
)
