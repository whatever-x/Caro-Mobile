package com.whatever.caro.core.importparser.xlsx

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import okio.Buffer
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8

internal sealed interface XmlEvent {
    /** 이름과 속성 이름은 접두사를 뗀 로컬 이름이다. */
    data class Start(
        val name: String,
        val attributes: Map<String, String>,
    ) : XmlEvent

    data class End(
        val name: String,
    ) : XmlEvent

    data class Text(
        val text: String,
    ) : XmlEvent
}

/**
 * xlsx의 OOXML 부분만 읽는 스트리밍 XML 리더.
 *
 * DTD가 필요 없는 형식이라 `<!DOCTYPE`과 그 밖의 `<!` 선언은 모두 거부한다. 엔티티 확장 공격과 외부 엔티티 참조를 막는 유일한 방어선이다.
 * 엔티티는 미리 정의된 5개와 숫자 문자 참조만 해석하고 나머지는 글자로 남긴다.
 */
internal class XmlPullReader(
    private val source: BufferedSource,
) {
    private var pendingEnd: XmlEvent.End? = null

    fun next(): XmlEvent? {
        pendingEnd?.let { end ->
            pendingEnd = null
            return end
        }
        while (!source.exhausted()) {
            if (source.buffer[0] != LT) return readText()
            source.skip(1)
            when (val first = source.readByte()) {
                QUESTION -> skipPast(PROCESSING_END)
                BANG -> readDeclaration()?.let { text -> return text }
                SLASH -> return XmlEvent.End(localName(readTagBody().trim()))
                else -> return readStart(first)
            }
        }
        return null
    }

    private fun readText(): XmlEvent.Text {
        val end = source.indexOf(LT)
        val raw = if (end < 0) source.readUtf8() else source.readUtf8(end)
        return XmlEvent.Text(XmlEntities.decode(raw))
    }

    private fun readDeclaration(): XmlEvent.Text? {
        if (source.rangeEquals(0, COMMENT_START)) {
            skipPast(COMMENT_END)
            return null
        }
        if (source.rangeEquals(0, CDATA_START)) {
            source.skip(CDATA_START.size.toLong())
            val end = source.indexOf(CDATA_END)
            if (end < 0) throw ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "unterminated CDATA")
            return XmlEvent.Text(source.readUtf8(end).also { source.skip(CDATA_END.size.toLong()) })
        }
        throw ImportParseException(ImportParseErrorKind.UNSUPPORTED_FILE, "XML declaration such as <!DOCTYPE is not allowed")
    }

    private fun readStart(first: Byte): XmlEvent.Start {
        val body = readTagBody(first)
        val selfClosing = body.endsWith('/')
        val content = if (selfClosing) body.dropLast(1) else body
        val nameEnd = content.indexOfFirst(Char::isWhitespace).let { index -> if (index < 0) content.length else index }
        val start = XmlEvent.Start(localName(content.substring(0, nameEnd)), XmlAttributes.parse(content, nameEnd))
        if (selfClosing) pendingEnd = XmlEvent.End(start.name)
        return start
    }

    /** `>`까지 읽는다. 속성 값 안의 `>`는 따옴표로 구분한다. */
    private fun readTagBody(startByte: Byte? = null): String {
        val tag = Buffer()
        var quote: Byte = 0
        startByte?.let { byte -> tag.writeByte(byte.toInt()) }
        while (true) {
            val byte = source.readByte()
            if (quote == NO_QUOTE && byte == GT) break
            if (byte == DOUBLE_QUOTE || byte == SINGLE_QUOTE) {
                quote =
                    when (quote) {
                        NO_QUOTE -> byte
                        byte -> NO_QUOTE
                        else -> quote
                    }
            }
            tag.writeByte(byte.toInt())
        }
        return tag.readUtf8()
    }

    private fun skipPast(terminator: okio.ByteString) {
        val end = source.indexOf(terminator)
        if (end < 0) throw ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "unterminated XML markup")
        source.skip(end + terminator.size)
    }

    private companion object {
        const val LT = '<'.code.toByte()
        const val GT = '>'.code.toByte()
        const val QUESTION = '?'.code.toByte()
        const val BANG = '!'.code.toByte()
        const val SLASH = '/'.code.toByte()
        const val DOUBLE_QUOTE = '"'.code.toByte()
        const val SINGLE_QUOTE = '\''.code.toByte()
        const val NO_QUOTE: Byte = 0
        val PROCESSING_END = "?>".encodeUtf8()
        val COMMENT_START = "--".encodeUtf8()
        val COMMENT_END = "-->".encodeUtf8()
        val CDATA_START = "[CDATA[".encodeUtf8()
        val CDATA_END = "]]>".encodeUtf8()
    }
}

internal fun localName(name: String): String = name.substringAfterLast(':')
