package com.whatever.caro.core.importparser.apkg

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.core.importparser.archive.ZipArchive
import okio.use

/**
 * Anki 패키지의 컬렉션 형식. 최신 패키지에는 안내 노트 1개만 든 더미 `collection.anki2`가 함께 있어
 * 파일 이름이 아니라 `meta`의 버전으로 읽을 컬렉션을 고른다.
 */
internal enum class AnkiPackageVersion(
    val collectionEntry: String,
    val zstdCompressed: Boolean,
) {
    LEGACY1("collection.anki2", zstdCompressed = false),
    LEGACY2("collection.anki21", zstdCompressed = false),
    LATEST("collection.anki21b", zstdCompressed = true),
    ;

    companion object {
        private const val META_ENTRY = "meta"
        private const val VERSION_FIELD = 1
        private const val LEGACY1_VERSION = 1L
        private const val LEGACY2_VERSION = 2L
        private const val LATEST_VERSION = 3L

        fun detect(archive: ZipArchive): AnkiPackageVersion {
            if (archive.contains(META_ENTRY)) {
                val meta = archive.open(META_ENTRY).use { source -> source.readByteArray() }
                return when (val version = ProtobufVarints.read(meta)[VERSION_FIELD]) {
                    LEGACY1_VERSION -> LEGACY1
                    LEGACY2_VERSION -> LEGACY2
                    LATEST_VERSION -> LATEST
                    else -> throw ImportParseException(ImportParseErrorKind.UNSUPPORTED_FILE, "unknown Anki package version $version")
                }
            }
            return when {
                archive.contains(LEGACY2.collectionEntry) -> LEGACY2
                archive.contains(LEGACY1.collectionEntry) -> LEGACY1
                else -> throw ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "no Anki collection entry")
            }
        }
    }
}
