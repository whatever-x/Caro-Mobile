package com.whatever.caro.core.remote.dto.notification.response

import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class TestPushResponse(
    /** 발송 성공 기기 수 */
    val successCount: Int?,
    /** 발송 실패 기기 수 */
    val failureCount: Int?,
    /** 무효 토큰으로 판단되어 삭제된 수 */
    val removedTokenCount: Int?,
)
