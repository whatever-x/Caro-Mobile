package com.whatever.caro.core.remote.dto.studySession.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
@SerialName("REST_DAY")
data object RestDay :
    DailyStudyResponse,
    DailyStudySummaryResponse
