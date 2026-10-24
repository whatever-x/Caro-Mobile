package com.whatever.caro.feature.importer.mvi

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf

/**
 * 다시 읽기에 넘기는 선택. 다시 읽기가 성공했을 때만 확정하므로 읽는 중에 뒤로 가면 바꾸기 전 값으로 되돌린다.
 *
 * @property subdeckIds 카드가 없는 노트 묶음을 고르면 `ParsedSubdeck.NO_DECK_ID`가 들어간다.
 * @property delimiter null이면 파서가 판별한 구분자를 쓴다.
 * @property hasHeaderRow null이면 파서가 추정한 값을 쓴다.
 */
data class ImportSelection(
    val subdeckIds: PersistentSet<String> = persistentSetOf(),
    val sheetIndex: Int = 0,
    val delimiter: Char? = null,
    val hasHeaderRow: Boolean? = null,
)
