package com.whatever.caro.core.model.card

import androidx.compose.runtime.Immutable

/**
 * 카드 한 장의 앞/뒷면 내용. 카드 생성 입력 모델로 사용한다.
 */
@Immutable
data class CardContent(
    val front: String,
    val back: String,
)
