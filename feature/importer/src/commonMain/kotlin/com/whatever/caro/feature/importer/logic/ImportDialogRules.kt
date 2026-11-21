package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.core.importparser.ImportSourceName
import com.whatever.caro.feature.importer.mvi.ImportDialog

internal enum class ParseErrorTitle {
    Unsupported,
    Corrupt,
    UnreadableText,
    ReadFailedFile,
    ReadFailedPaste,
}

internal enum class ParseErrorBody {
    UnsupportedXls,
    UnsupportedXlsx,
    UnsupportedApkg,
    UnsupportedGeneric,
    CorruptApkg,
    CorruptXlsx,
    EncodingUndetermined,
    ReadFailedFile,
    ReadFailedPaste,
    TooManyColumns,
}

/** 문구는 리소스 키 역할의 enum으로만 돌려주고 문자열 선택은 화면이 한다. */
internal data class ParseErrorCopy(
    val title: ParseErrorTitle,
    val body: ParseErrorBody,
)

/** 확장자를 크기보다 먼저 본다. 지원하지 않는 확장자의 큰 파일이 크기 안내로 가면 줄여도 다시 막히기 때문이다. */
internal fun tooLargePickDialog(displayName: String): ImportDialog {
    val name = sanitizeDisplayName(displayName)
    val extension = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return if (ImportSourceName.supportedExtension(name) == null) {
        ImportDialog.ParseError(ImportParseErrorKind.UNSUPPORTED_FILE, extension, isPaste = false)
    } else {
        ImportDialog.FileTooLarge(extension)
    }
}

/** 파서가 던진 오류만 종류를 가리고, 그 밖의 실패는 파일을 읽지 못했다는 문구가 나오는 종류로 보낸다. */
internal fun readFailureDialog(
    error: Throwable,
    extension: String,
    isPaste: Boolean,
): ImportDialog {
    val kind = (error as? ImportParseException)?.kind ?: ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED
    return if (kind == ImportParseErrorKind.FILE_TOO_LARGE) {
        ImportDialog.FileTooLarge(extension)
    } else {
        ImportDialog.ParseError(kind, extension, isPaste)
    }
}

/** 선택기 실패 전용 오류 종류가 없어, 확장자와 상관없이 파일을 읽지 못했다는 문구가 나오는 종류를 쓴다. */
internal fun pickFailedDialog(): ImportDialog.ParseError =
    ImportDialog.ParseError(ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED, extension = "", isPaste = false)

internal fun parseErrorCopy(dialog: ImportDialog.ParseError): ParseErrorCopy {
    val isTooManyColumns = dialog.kind == ImportParseErrorKind.TOO_MANY_COLUMNS
    if (dialog.isPaste) {
        val body = if (isTooManyColumns) ParseErrorBody.TooManyColumns else ParseErrorBody.ReadFailedPaste
        return ParseErrorCopy(ParseErrorTitle.ReadFailedPaste, body)
    }
    return when (dialog.kind) {
        ImportParseErrorKind.UNSUPPORTED_FILE -> ParseErrorCopy(ParseErrorTitle.Unsupported, unsupportedBody(dialog.extension))

        ImportParseErrorKind.CORRUPT_FILE -> corruptCopy(dialog.extension)

        ImportParseErrorKind.ENCODING_UNDETERMINED -> ParseErrorCopy(ParseErrorTitle.UnreadableText, ParseErrorBody.EncodingUndetermined)

        ImportParseErrorKind.TOO_MANY_COLUMNS -> ParseErrorCopy(ParseErrorTitle.ReadFailedFile, ParseErrorBody.TooManyColumns)

        // FILE_TOO_LARGE는 readFailureDialog가 FileTooLarge로 바꾸므로 여기 오지 않는다. when을 빠짐없이 두기 위해 함께 묶는다.
        ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED,
        ImportParseErrorKind.CONTENT_TOO_LONG,
        ImportParseErrorKind.FILE_TOO_LARGE,
        -> ParseErrorCopy(ParseErrorTitle.ReadFailedFile, ParseErrorBody.ReadFailedFile)
    }
}

private fun unsupportedBody(extension: String): ParseErrorBody =
    when (extension) {
        "xls" -> ParseErrorBody.UnsupportedXls
        "xlsx" -> ParseErrorBody.UnsupportedXlsx
        "apkg" -> ParseErrorBody.UnsupportedApkg
        else -> ParseErrorBody.UnsupportedGeneric
    }

/** 손상 문구는 apkg와 xlsx만 쓴다. 손상된 텍스트 파일은 파일을 읽지 못했다는 문구로 보낸다. */
private fun corruptCopy(extension: String): ParseErrorCopy =
    when (extension) {
        "apkg" -> ParseErrorCopy(ParseErrorTitle.Corrupt, ParseErrorBody.CorruptApkg)
        "xlsx" -> ParseErrorCopy(ParseErrorTitle.Corrupt, ParseErrorBody.CorruptXlsx)
        else -> ParseErrorCopy(ParseErrorTitle.ReadFailedFile, ParseErrorBody.ReadFailedFile)
    }
