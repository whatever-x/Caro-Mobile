package com.whatever.caro.core.model.importing

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * 노트 유형 하나의 "원본 항목 이름 → 카드 항목" 연결. 카드 항목 키를 맵으로 둬 이후 커스텀 항목을 추가해도 구조가 바뀌지 않는다.
 *
 * @property targets 카드 항목 키별 원본 항목 이름 목록. 목록 순서가 사용자가 고른 순서이고 그 순서대로 줄바꿈으로 합친다.
 */
@Immutable
@Serializable
data class FieldMapping(
    val noteTypeId: String,
    val targets: Map<String, List<String>>,
    val excluded: Boolean = false,
) {
    val frontFields: List<String> get() = targets[FRONT].orEmpty()
    val backFields: List<String> get() = targets[BACK].orEmpty()

    companion object {
        const val FRONT = "front"
        const val BACK = "back"
    }
}
