package com.whatever.caro.core.model.card

/**
 * 카드 한 장의 앞/뒷면 내용. 카드 생성 입력 모델로 사용한다.
 */
data class CardContent(
    val front: String,
    val back: String,
)
