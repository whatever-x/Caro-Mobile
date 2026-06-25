package com.whatever.caro.core.remote.dto.studySession.response

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

// AUTO-GENERATED FROM SWAGGER — 직접 수정하지 마세요
@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
sealed interface DailyStudyResponse
