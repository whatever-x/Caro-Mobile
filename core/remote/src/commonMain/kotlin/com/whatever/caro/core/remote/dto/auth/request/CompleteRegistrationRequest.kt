package com.whatever.caro.core.remote.dto.auth.request

import kotlinx.serialization.Serializable

@Serializable
data class CompleteRegistrationRequest(
    val nickname: String,
    val termsAgreed: Boolean,
)
