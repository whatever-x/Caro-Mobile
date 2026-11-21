package com.whatever.caro.core.importparser.text

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException

/** 디코딩된 텍스트 조각을 이어서 한 글자씩 읽는다. 파일 전체를 한 문자열로 만들지 않기 위해서다. */
internal class CharCursor(
    private val chunks: Iterator<String>,
) {
    private var current = ""
    private var position = 0

    fun peek(): Int = if (fill()) current[position].code else END

    fun next(): Int = if (fill()) current[position++].code else END

    private fun fill(): Boolean {
        while (position >= current.length) {
            if (!chunks.hasNext()) return false
            current = chunks.next()
            position = 0
        }
        return true
    }

    companion object {
        const val END = -1
    }
}

/**
 * RFC 4180 레코드를 읽는다. 따옴표 안의 구분자와 줄바꿈을 값으로 두고 `""`는 따옴표 하나로 바꾼다.
 * 닫는 따옴표 뒤의 글자와 닫히지 않은 따옴표는 오류로 멈추지 않고 값에 붙인다(Python csv 기본 동작과 같다).
 */
internal class DelimitedRecordReader(
    private val cursor: CharCursor,
    private val delimiter: Char,
    private val maxFields: Int = Int.MAX_VALUE,
    private val maxFieldChars: Int = Int.MAX_VALUE,
) {
    private enum class State { START_RECORD, START_FIELD, IN_FIELD, IN_QUOTED, QUOTE_IN_QUOTED }

    private val fields = mutableListOf<String>()
    private val field = StringBuilder()

    // 빈 값은 다음에 값이 나올 때까지 개수만 세어 둔다. 끝에만 빈 구분자가 수천 개 이어진 행이 열 상한에 걸리지 않게 하기 위해서다.
    private var pendingEmpty = 0

    /** 빈 줄은 건너뛰고 다음 레코드를 돌려준다. 끝이면 null이다. */
    fun nextRecord(): List<String>? {
        while (cursor.peek() != CharCursor.END) {
            val record = readRecord()
            if (record.isNotEmpty()) return record
        }
        return null
    }

    private fun readRecord(): List<String> {
        fields.clear()
        field.clear()
        pendingEmpty = 0
        var state = State.START_RECORD
        while (true) {
            val code = cursor.next()
            if (code == CharCursor.END) return finishAtEnd(state)
            val next = step(state, code.toChar()) ?: return endRecord(code.toChar())
            state = next
        }
    }

    /** 레코드가 끝나면 null을 돌려준다. */
    private fun step(
        state: State,
        char: Char,
    ): State? =
        when (state) {
            State.START_RECORD -> if (isNewline(char)) null else startField(char)
            State.START_FIELD -> if (isNewline(char)) saveThen(null) else startField(char)
            State.IN_FIELD -> inField(char)
            State.IN_QUOTED -> if (char == QUOTE) State.QUOTE_IN_QUOTED else appendThen(char, State.IN_QUOTED)
            State.QUOTE_IN_QUOTED -> quoteInQuoted(char)
        }

    private fun startField(char: Char): State =
        when (char) {
            QUOTE -> State.IN_QUOTED
            delimiter -> saveThen(State.START_FIELD) ?: State.START_FIELD
            else -> appendThen(char, State.IN_FIELD)
        }

    private fun inField(char: Char): State? =
        when {
            isNewline(char) -> saveThen(null)
            char == delimiter -> saveThen(State.START_FIELD)
            else -> appendThen(char, State.IN_FIELD)
        }

    private fun quoteInQuoted(char: Char): State? =
        when {
            char == QUOTE -> appendThen(QUOTE, State.IN_QUOTED)
            char == delimiter -> saveThen(State.START_FIELD)
            isNewline(char) -> saveThen(null)
            else -> appendThen(char, State.IN_FIELD)
        }

    // 닫히지 않은 따옴표 하나가 파일 나머지를 한 값으로 삼켜 메모리를 다 쓰지 않도록 값 길이와 열 수를 읽는 중에 막는다.
    private fun appendThen(
        char: Char,
        next: State,
    ): State {
        if (field.length >= maxFieldChars) {
            throw ImportParseException(ImportParseErrorKind.CONTENT_TOO_LONG, "field over $maxFieldChars chars")
        }
        field.append(char)
        return next
    }

    private fun saveThen(next: State?): State? {
        if (field.isEmpty()) {
            pendingEmpty++
        } else {
            if (fields.size + pendingEmpty >= maxFields) {
                throw ImportParseException(ImportParseErrorKind.TOO_MANY_COLUMNS, "record over $maxFields fields")
            }
            repeat(pendingEmpty) { fields += "" }
            pendingEmpty = 0
            fields += field.toString()
        }
        field.clear()
        return next
    }

    /** 끝의 빈 값은 원래 열 수가 상한 안일 때만 남겨, 일반 파일의 열 수와 열 맞춤 경고 수는 그대로 둔다. */
    private fun completeRecord(): List<String> {
        if (fields.size + pendingEmpty <= maxFields) repeat(pendingEmpty) { fields += "" }
        pendingEmpty = 0
        return fields.toList()
    }

    private fun endRecord(char: Char): List<String> {
        if (char == CR && cursor.peek() == LF.code) cursor.next()
        return completeRecord()
    }

    private fun finishAtEnd(state: State): List<String> {
        if (state != State.START_RECORD) saveThen(null)
        return completeRecord()
    }

    private fun isNewline(char: Char): Boolean = char == LF || char == CR

    private companion object {
        const val QUOTE = '"'
        const val LF = '\n'
        const val CR = '\r'
    }
}
