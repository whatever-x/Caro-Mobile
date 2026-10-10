package com.whatever.caro.core.importparser

class ImportParseException(
    val kind: ImportParseErrorKind,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

enum class ImportParseErrorKind {
    FILE_TOO_LARGE,
    UNSUPPORTED_FILE,
    CORRUPT_FILE,
    ARCHIVE_LIMIT_EXCEEDED,
    ENCODING_UNDETERMINED,
}
