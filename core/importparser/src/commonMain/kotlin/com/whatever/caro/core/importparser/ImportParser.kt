package com.whatever.caro.core.importparser

import androidx.sqlite.SQLiteException
import com.whatever.caro.core.importparser.apkg.ApkgReader
import com.whatever.caro.core.importparser.detect.DetectedSource
import com.whatever.caro.core.importparser.detect.ImportFormatDetector
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.importparser.text.TextFileReader
import com.whatever.caro.core.importparser.text.TextTableReader
import com.whatever.caro.core.importparser.xlsx.XlsxReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import okio.FileSystem
import okio.IOException
import okio.Path

/**
 * 가져오기 원본을 [ParsedTable]로 읽는 진입점. 파일 IO와 SQLite 호출이 블로킹이므로 호출자가 IO 디스패처에서 부른다.
 *
 * 실패는 모두 [ImportParseException]으로 바꿔 던진다. 취소는 그대로 전파한다.
 */
class ImportParser(
    private val fileSystem: FileSystem,
) {
    /** [sourceName]은 사용자에게 보인 파일 이름이다. 임시 사본 경로에는 확장자가 없을 수 있어 형식 판별에 이 이름을 쓴다. */
    suspend fun parseFile(
        path: Path,
        sourceName: String,
        options: ParseOptions,
    ): ParsedTable =
        translateFailures {
            // 지원하지 않는 확장자는 크기와 상관없이 같은 오류여야 해서 크기 확인보다 먼저 거른다.
            val extension =
                ImportSourceName.supportedExtension(sourceName)
                    ?: throw ImportParseException(ImportParseErrorKind.UNSUPPORTED_FILE, "unsupported or missing extension")
            val fileBytes = fileSystem.metadata(path).size ?: throw ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "size unknown")
            // 크기만 보고 내용을 읽기 전에 멈춘다.
            if (fileBytes > options.maxFileBytes) {
                throw ImportParseException(ImportParseErrorKind.FILE_TOO_LARGE, "file is $fileBytes bytes")
            }
            // 압축 해제와 SQLite 반복문은 블로킹이라 취소를 스스로 확인해야 Back 뒤에 계속 돌며 메모리와 CPU를 쓰지 않는다.
            val job = currentCoroutineContext().job
            when (val source = ImportFormatDetector.detect(fileSystem, path, extension, fileBytes, options, job::ensureActive)) {
                is DetectedSource.Apkg -> ApkgReader(fileSystem, source.archive, options).read(sourceName)
                is DetectedSource.Xlsx -> XlsxReader(source.archive, options).read(sourceName)
                DetectedSource.Text -> TextFileReader(fileSystem, options).read(path, sourceName)
            }
        }

    /** 붙여넣은 글은 이미 문자열이라 인코딩 판별 없이 텍스트 파일과 같은 규칙으로 읽는다. */
    suspend fun parseText(
        text: String,
        sourceName: String,
        options: ParseOptions,
    ): ParsedTable = translateFailures { TextTableReader(options).read(listOf(text).iterator(), sourceName) }

    private suspend fun translateFailures(block: suspend () -> ParsedTable): ParsedTable =
        try {
            block()
        } catch (exception: ImportParseException) {
            throw exception
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: IOException) {
            throw ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "read failed", exception)
        } catch (exception: SQLiteException) {
            throw ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "Anki collection unreadable", exception)
        } catch (exception: IllegalArgumentException) {
            throw ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "unexpected file content", exception)
        } catch (exception: IllegalStateException) {
            throw ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "unexpected file content", exception)
        }
}
