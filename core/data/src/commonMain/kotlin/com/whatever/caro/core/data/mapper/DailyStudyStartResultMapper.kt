package com.whatever.caro.core.data.mapper

import com.whatever.caro.core.model.learning.ActiveDailyStudySession
import com.whatever.caro.core.model.learning.DailyStudyStartResult
import com.whatever.caro.core.model.learning.StudyCard
import com.whatever.caro.core.remote.dto.studySession.response.Completed
import com.whatever.caro.core.remote.dto.studySession.response.DailyStudyResponse
import com.whatever.caro.core.remote.dto.studySession.response.InProgress
import com.whatever.caro.core.remote.dto.studySession.response.RestDay

private const val FIELD_FRONT = "front"
private const val FIELD_BACK = "back"

internal fun DailyStudyResponse.toDailyStudyStartResult(): DailyStudyStartResult =
    when (this) {
        is InProgress -> {
            DailyStudyStartResult.Started(
                ActiveDailyStudySession(
                    sessionId = requireNotNull(sessionId),
                    studiedCardCount = studiedCardCount ?: 0,
                    totalCardCount = totalCardCount ?: 0,
                    cards =
                        cards.orEmpty().map {
                            StudyCard(
                                requireNotNull(it.cardId),
                                it.fields?.get(FIELD_FRONT).orEmpty(),
                                it.fields?.get(FIELD_BACK).orEmpty(),
                            )
                        },
                ),
            )
        }

        is Completed -> {
            DailyStudyStartResult.Completed(studiedCardCount ?: 0, totalCardCount ?: 0)
        }

        RestDay -> {
            DailyStudyStartResult.RestDay
        }
    }
