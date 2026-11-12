package com.whatever.caro.core.remote.dto.auth.request

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
data class CompleteRegistrationV11Request(
    val nickname: String,
    /** 생년월일. 성인 여부 판정에만 사용하며 저장하지 않는다 */
    val birthDate: LocalDate,
    /** 약관 동의 여부 */
    val isTermsAgreed: Boolean,
)
