package com.whatever.caro.core.importparser.apkg

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.core.importparser.archive.ZipArchive
import okio.BufferedSource
import okio.use

/** 최신 패키지에는 안내 노트 1개만 든 더미 `collection.anki2`가 함께 있어 파일 이름이 아니라 `meta`의 버전으로 읽을 컬렉션을 고른다. */
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

        // 실제 meta는 수십 바이트다. 이보다 크면 버전 하나를 읽으려고 큰 항목을 메모리에 올리지 않고 손상으로 본다.
        private const val MAX_META_BYTES = 64L * 1024

        private fun readMeta(source: BufferedSource): ByteArray {
            if (source.request(
                    MAX_META_BYTES + 1,
                )
            ) {
                throw ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "meta over $MAX_META_BYTES bytes")
            }
            return source.readByteArray()
        }

        fun detect(archive: ZipArchive): AnkiPackageVersion {
            if (archive.contains(META_ENTRY)) {
                val meta = archive.open(META_ENTRY).use { source -> readMeta(source) }
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
