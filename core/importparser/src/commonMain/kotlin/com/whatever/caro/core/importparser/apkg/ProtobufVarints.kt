package com.whatever.caro.core.importparser.apkg

/**
 * Anki의 `meta`와 노트 유형 `config`에서 필요한 값이 모두 varint라 전체 protobuf 라이브러리를 들이지 않는다.
 * 형식이 깨졌으면 그때까지 읽은 값만 돌려준다.
 */
internal object ProtobufVarints {
    private const val WIRE_VARINT = 0
    private const val WIRE_FIXED64 = 1
    private const val WIRE_LENGTH_DELIMITED = 2
    private const val WIRE_FIXED32 = 5
    private const val FIXED64_BYTES = 8
    private const val FIXED32_BYTES = 4
    private const val WIRE_TYPE_BITS = 3
    private const val WIRE_TYPE_MASK = 0x7
    private const val VARINT_PAYLOAD_MASK = 0x7F
    private const val VARINT_CONTINUE = 0x80
    private const val VARINT_SHIFT = 7
    private const val MAX_VARINT_SHIFT = 63
    private const val BYTE_MASK = 0xFF

    fun read(bytes: ByteArray): Map<Int, Long> {
        val fields = HashMap<Int, Long>()
        var position = 0
        while (position < bytes.size) {
            val (key, afterKey) = varint(bytes, position) ?: return fields
            val field = (key ushr WIRE_TYPE_BITS).toInt()
            position =
                when ((key and WIRE_TYPE_MASK.toLong()).toInt()) {
                    WIRE_VARINT -> {
                        val (value, next) = varint(bytes, afterKey) ?: return fields
                        fields[field] = value
                        next
                    }

                    WIRE_FIXED64 -> {
                        afterKey + FIXED64_BYTES
                    }

                    WIRE_LENGTH_DELIMITED -> {
                        val (length, next) = varint(bytes, afterKey) ?: return fields
                        if (length < 0 || length > bytes.size - next) return fields
                        next + length.toInt()
                    }

                    WIRE_FIXED32 -> {
                        afterKey + FIXED32_BYTES
                    }

                    else -> {
                        return fields
                    }
                }
        }
        return fields
    }

    private fun varint(
        bytes: ByteArray,
        start: Int,
    ): Pair<Long, Int>? {
        var result = 0L
        var shift = 0
        var position = start
        while (position < bytes.size && shift <= MAX_VARINT_SHIFT) {
            val byte = bytes[position++].toInt() and BYTE_MASK
            result = result or ((byte and VARINT_PAYLOAD_MASK).toLong() shl shift)
            if (byte and VARINT_CONTINUE == 0) return result to position
            shift += VARINT_SHIFT
        }
        return null
    }
}
