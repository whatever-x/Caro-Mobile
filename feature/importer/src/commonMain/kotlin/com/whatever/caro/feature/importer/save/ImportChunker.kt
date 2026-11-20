package com.whatever.caro.feature.importer.save

import com.whatever.caro.core.model.card.CardContent

/** 저장 직전에 동결한 묶음. 재시도에는 이 [cards] 객체와 [idempotencyKey]를 그대로 넘겨 같은 본문 바이트를 보낸다. */
internal data class ImportChunk(
    val index: Int,
    val cards: List<CardContent>,
    val idempotencyKey: String,
)

internal object ImportChunker {
    // `{"items":[` + `]}`
    private const val REQUEST_OVERHEAD_BYTES = 16L

    // `{"fields":{"front":"","back":""},"cardType":"BASIC"},`보다 넉넉하게 잡는다.
    private const val ITEM_OVERHEAD_BYTES = 64L
    private const val ESCAPED_CHAR_BYTES = 2L
    private const val UNICODE_ESCAPE_BYTES = 6L
    private const val FIRST_PRINTABLE = 0x20
    private const val ONE_BYTE_LIMIT = 0x80
    private const val TWO_BYTE_LIMIT = 0x800
    private const val SURROGATE_HALF_BYTES = 2L
    private const val THREE_BYTES = 3L

    /** 장수 상한과 직렬화 본문 추정 바이트 상한을 모두 지키도록 원본 순서대로 나눈다. 한 장이 바이트 상한을 넘으면 그 카드만 담는다. */
    fun split(
        cards: List<CardContent>,
        maxItems: Int,
        maxBytes: Long,
        newKey: () -> String,
    ): List<ImportChunk> {
        val chunks = mutableListOf<ImportChunk>()
        var current = mutableListOf<CardContent>()
        var currentBytes = REQUEST_OVERHEAD_BYTES
        cards.forEach { card ->
            val itemBytes = estimatedItemBytes(card)
            if (current.isNotEmpty() && (current.size >= maxItems || currentBytes + itemBytes > maxBytes)) {
                chunks += ImportChunk(chunks.size, current.toList(), newKey())
                current = mutableListOf()
                currentBytes = REQUEST_OVERHEAD_BYTES
            }
            current += card
            currentBytes += itemBytes
        }
        if (current.isNotEmpty()) chunks += ImportChunk(chunks.size, current.toList(), newKey())
        return chunks
    }

    /** 실제 JSON보다 작게 잡지 않도록 제어 문자는 `\u00XX` 길이로 센다. */
    fun estimatedRequestBytes(cards: List<CardContent>): Long = REQUEST_OVERHEAD_BYTES + cards.sumOf(::estimatedItemBytes)

    private fun estimatedItemBytes(card: CardContent): Long =
        ITEM_OVERHEAD_BYTES + escapedUtf8Bytes(card.front) + escapedUtf8Bytes(card.back)

    private fun escapedUtf8Bytes(text: String): Long =
        text.sumOf { char ->
            when {
                char == '"' || char == '\\' -> ESCAPED_CHAR_BYTES
                char.code < FIRST_PRINTABLE -> UNICODE_ESCAPE_BYTES
                char.code < ONE_BYTE_LIMIT -> 1L
                char.code < TWO_BYTE_LIMIT -> 2L
                char.isSurrogate() -> SURROGATE_HALF_BYTES
                else -> THREE_BYTES
            }
        }
}
