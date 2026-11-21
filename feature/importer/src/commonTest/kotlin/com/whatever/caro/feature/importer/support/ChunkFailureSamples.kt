package com.whatever.caro.feature.importer.support

import com.whatever.caro.feature.importer.save.ChunkFailure

// commonTest에서는 sealedSubclasses를 쓸 수 없어 표본을 직접 나열한다.
// 변형을 추가하면 isUnconfirmedSample의 when이 컴파일을 깨므로, 거기서 분류를 정하고 이 목록에도 표본을 넣어야 분류 테스트와 Paused 불변식 테스트가 그 변형을 검사한다.
private val FAILURE_SAMPLES: List<ChunkFailure> =
    listOf(
        ChunkFailure.RetryExhausted,
        ChunkFailure.ProcessingTimeout,
        ChunkFailure.InvalidResponse,
        ChunkFailure.Unknown,
        ChunkFailure.KeyConflict,
        ChunkFailure.StoppedAfterFailedSend,
        ChunkFailure.Rejected("C001"),
        ChunkFailure.ResponseCountMismatch(created = 1),
        ChunkFailure.DeckUnavailable,
        ChunkFailure.StoppedByUser,
    )

internal val UNCONFIRMED_FAILURE_SAMPLES: List<ChunkFailure> = FAILURE_SAMPLES.filter(::isUnconfirmedSample)

internal val CONFIRMED_FAILURE_SAMPLES: List<ChunkFailure> = FAILURE_SAMPLES.filterNot(::isUnconfirmedSample)

// 운영 코드의 isSaveUnconfirmed를 쓰면 분류 테스트가 자기 자신과 비교하므로 기대값을 따로 적는다.
// else를 두면 새 변형이 컴파일 오류 없이 한쪽 목록에 들어간다.
private fun isUnconfirmedSample(failure: ChunkFailure): Boolean =
    when (failure) {
        ChunkFailure.RetryExhausted,
        ChunkFailure.ProcessingTimeout,
        ChunkFailure.InvalidResponse,
        ChunkFailure.Unknown,
        ChunkFailure.KeyConflict,
        ChunkFailure.StoppedAfterFailedSend,
        -> true

        is ChunkFailure.Rejected,
        is ChunkFailure.ResponseCountMismatch,
        ChunkFailure.DeckUnavailable,
        ChunkFailure.StoppedByUser,
        -> false
    }
