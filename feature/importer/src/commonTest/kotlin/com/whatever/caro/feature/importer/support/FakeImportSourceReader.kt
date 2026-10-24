package com.whatever.caro.feature.importer.support

import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.source.ImportSourceReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okio.ByteString
import okio.FileSystem
import okio.Path

internal sealed interface ReadCall {
    val sourceName: String
    val dateStyle: SpreadsheetDateStyle
    val delimiter: Char?
    val hasHeaderRow: Boolean?

    /** [copiedBytes]는 호출 시점의 사본 내용이다. 읽은 뒤 사본이 지워져도 무엇을 읽으려 했는지 남기려고 따로 담는다. */
    data class FileRead(
        val path: Path,
        override val sourceName: String,
        override val dateStyle: SpreadsheetDateStyle,
        val subdeckIds: Set<String>?,
        val sheetIndex: Int,
        override val delimiter: Char?,
        override val hasHeaderRow: Boolean?,
        val copiedBytes: ByteString?,
    ) : ReadCall

    data class TextRead(
        val text: String,
        override val sourceName: String,
        override val dateStyle: SpreadsheetDateStyle,
        override val delimiter: Char?,
        override val hasHeaderRow: Boolean?,
    ) : ReadCall
}

/**
 * 대본대로 표를 돌려주거나, 던지거나, [suspends]가 돌려준 게이트가 열릴 때까지 멈춘다.
 * 대본이 비었을 때 [fallback]이 있으면 실제 파서로 읽고, 없으면 테스트가 잘못 짜인 것이므로 실패시킨다.
 */
internal class FakeImportSourceReader(
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val fallback: ImportSourceReader? = null,
) : ImportSourceReader {
    private sealed interface Reply {
        data class Returns(
            val table: ParsedTable,
        ) : Reply

        data class Fails(
            val error: Throwable,
        ) : Reply

        data class Suspends(
            val gate: CompletableDeferred<ParsedTable>,
            val ignoresCancellation: Boolean,
        ) : Reply
    }

    private val replies = ArrayDeque<Reply>()
    private val recordedCalls = mutableListOf<ReadCall>()

    val calls: List<ReadCall> get() = recordedCalls.toList()

    /** 게이트를 기다리다 취소된 읽기 수. 작업 취소가 실제로 일어났는지 확인하는 데 쓴다. */
    var cancelledReads: Int = 0
        private set

    fun returns(table: ParsedTable): FakeImportSourceReader = apply { replies.addLast(Reply.Returns(table)) }

    fun fails(error: Throwable): FakeImportSourceReader = apply { replies.addLast(Reply.Fails(error)) }

    fun suspends(): CompletableDeferred<ParsedTable> =
        CompletableDeferred<ParsedTable>().also { gate -> replies.addLast(Reply.Suspends(gate, ignoresCancellation = false)) }

    /** 블로킹 파서 단계처럼 취소 요청을 받고도 게이트가 열릴 때까지 계속 도는 읽기를 만든다. */
    fun suspendsIgnoringCancellation(): CompletableDeferred<ParsedTable> =
        CompletableDeferred<ParsedTable>().also { gate -> replies.addLast(Reply.Suspends(gate, ignoresCancellation = true)) }

    override suspend fun readFile(
        path: Path,
        sourceName: String,
        dateStyle: SpreadsheetDateStyle,
        subdeckIds: Set<String>?,
        sheetIndex: Int,
        delimiter: Char?,
        hasHeaderRow: Boolean?,
    ): ParsedTable {
        recordedCalls +=
            ReadCall.FileRead(
                path = path,
                sourceName = sourceName,
                dateStyle = dateStyle,
                subdeckIds = subdeckIds,
                sheetIndex = sheetIndex,
                delimiter = delimiter,
                hasHeaderRow = hasHeaderRow,
                copiedBytes = if (fileSystem.exists(path)) fileSystem.read(path) { readByteString() } else null,
            )
        return answer {
            fallback?.readFile(path, sourceName, dateStyle, subdeckIds, sheetIndex, delimiter, hasHeaderRow)
        }
    }

    override suspend fun readText(
        text: String,
        sourceName: String,
        dateStyle: SpreadsheetDateStyle,
        delimiter: Char?,
        hasHeaderRow: Boolean?,
    ): ParsedTable {
        recordedCalls += ReadCall.TextRead(text, sourceName, dateStyle, delimiter, hasHeaderRow)
        return answer { fallback?.readText(text, sourceName, dateStyle, delimiter, hasHeaderRow) }
    }

    private suspend fun answer(fallbackRead: suspend () -> ParsedTable?): ParsedTable =
        when (val reply = replies.removeFirstOrNull()) {
            null -> fallbackRead() ?: error("대본에 없는 읽기 호출이다. calls=${recordedCalls.size}")
            is Reply.Returns -> reply.table
            is Reply.Fails -> throw reply.error
            is Reply.Suspends -> awaitGate(reply)
        }

    private suspend fun awaitGate(reply: Reply.Suspends): ParsedTable =
        if (reply.ignoresCancellation) {
            withContext(NonCancellable) { reply.gate.await() }
        } else {
            try {
                reply.gate.await()
            } catch (cancellation: CancellationException) {
                cancelledReads += 1
                throw cancellation
            }
        }
}
