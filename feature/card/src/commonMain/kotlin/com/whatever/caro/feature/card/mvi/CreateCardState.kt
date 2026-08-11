package com.whatever.caro.feature.card.mvi

import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.card.CardInputLimits
import com.whatever.caro.core.viewmodel.contract.UiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CreateCardState(
    val front: String = "",
    val back: String = "",
    val addedCards: ImmutableList<StagedCard> = persistentListOf(),
    val nextCardId: Long = 0L,
    val isSaving: Boolean = false,
    val isDiscardDialogVisible: Boolean = false,
) : UiState {
    val frontCount: String
        get() = "${front.length}/${CardInputLimits.FIELD_MAX}"

    val backCount: String
        get() = "${back.length}/${CardInputLimits.FIELD_MAX}"

    val isFrontMaxReached: Boolean
        get() = front.length >= CardInputLimits.FIELD_MAX

    val isBackMaxReached: Boolean
        get() = back.length >= CardInputLimits.FIELD_MAX

    val addedCount: Int
        get() = addedCards.size

    val isMaxCardsReached: Boolean
        get() = addedCards.size >= CardInputLimits.MAX_CARDS

    /** 뒤로가기 시 잃을 입력이 있는지. 스테이징된 카드 또는 작성 중인 텍스트. */
    val hasUnsavedInput: Boolean
        get() = addedCards.isNotEmpty() || front.isNotBlank() || back.isNotBlank()

    val isAddEnabled: Boolean
        get() =
            front.isNotBlank() &&
                back.isNotBlank() &&
                isSaving.not()

    val isSaveEnabled: Boolean
        get() = addedCards.isNotEmpty() && isSaving.not()
}

/**
 * 추가된 카드 미리보기 항목.
 * LazyRow 안정 key 와 인덱스 무관 삭제(중복 탭 방어)를 위해 고유 [id] 를 가진다.
 */
data class StagedCard(
    val id: Long,
    val content: CardContent,
)
