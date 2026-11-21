package com.whatever.caro.feature.importer.logic

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val PasteInputMinHeight = 120.dp

/** [inputHeightWithGuide]는 제목과 안내를 보인 채 잰 높이다. 숨긴 뒤의 높이로 판정하면 숨김과 보임이 번갈아 바뀐다. */
internal fun shouldHidePasteGuide(
    isImeVisible: Boolean,
    inputHeightWithGuide: Dp,
): Boolean = isImeVisible && inputHeightWithGuide < PasteInputMinHeight
