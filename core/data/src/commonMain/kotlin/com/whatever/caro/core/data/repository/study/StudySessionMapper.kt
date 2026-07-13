package com.whatever.caro.core.data.repository.study

import com.whatever.caro.core.model.study.StudyCard
import com.whatever.caro.core.model.study.StudySession
import com.whatever.caro.core.remote.dto.studySession.response.Completed
import com.whatever.caro.core.remote.dto.studySession.response.DailyStudyResponse
import com.whatever.caro.core.remote.dto.studySession.response.InProgress
import com.whatever.caro.core.remote.dto.studySession.response.RestDay

internal fun DailyStudyResponse.toModel(): StudySession =
    when (this) {
        is InProgress -> {
            StudySession.InProgress(
                sessionId = requireNotNull(sessionId),
                studiedCardCount = studiedCardCount ?: 0,
                totalCardCount = totalCardCount ?: 0,
                cards =
                    cards.orEmpty().map {
                        StudyCard(
                            requireNotNull(it.cardId),
                            it.fields?.get("front").orEmpty(),
                            it.fields?.get("back").orEmpty(),
                        )
                    },
            )
        }

        is Completed -> {
            StudySession.Completed(studiedCardCount ?: 0, totalCardCount ?: 0)
        }

        RestDay -> {
            StudySession.RestDay
        }
    }
