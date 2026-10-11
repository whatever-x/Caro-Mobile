package com.whatever.caro.feature.importer.source

import okio.FileSystem
import okio.IOException
import okio.Path
import kotlin.random.Random

private const val SESSION_PREFIX = "session-"

// 겹쳐 열린 다른 화면이 쓰는 디렉터리를 지우지 않도록 충분히 오래된 것만 지운다.
private const val STALE_AFTER_MILLIS = 60L * 60 * 1000

/**
 * 선택한 원본 사본과 파서의 작업 디렉터리를 [root] 한 곳에 모은다. 화면마다 [root]가 달라, 한 화면의 정리가 겹쳐 열린 다른 화면의 파일을 지우지 않는다.
 * 파서의 작업 디렉터리도 원본 사본이 있는 [root] 아래에 만들어야 함께 지워진다.
 */
internal class ImportTempStorage(
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val root: Path = DEFAULT_ROOT / newSessionName(),
    private val staleAfterMillis: Long = STALE_AFTER_MILLIS,
) {
    /** 정리 실패 때문에 가져오기를 막지 않는다. 남은 파일은 다음 정리에서 지운다. */
    fun clearAll() {
        try {
            fileSystem.deleteRecursively(root, mustExist = false)
        } catch (_: IOException) {
            return
        }
    }

    /**
     * 앱이 종료되며 정리하지 못한 이전 화면의 디렉터리를 지운다. [root]는 건드리지 않는다.
     * 앱의 시계와 파일 시각이 어긋날 수 있어(iOS 시뮬레이터에서 확인) 방금 만든 [root]의 수정 시각을 현재로 쓴다.
     */
    fun clearStale() {
        val parent = root.parent ?: return
        try {
            fileSystem.createDirectories(root)
            val now = fileSystem.metadataOrNull(root)?.lastModifiedAtMillis ?: return
            fileSystem
                .listOrNull(parent)
                .orEmpty()
                .filter { child -> child != root && child.name.startsWith(SESSION_PREFIX) }
                .filter { child -> fileSystem.metadataOrNull(child)?.lastModifiedAtMillis?.let { now - it >= staleAfterMillis } == true }
                .forEach { child -> fileSystem.deleteRecursively(child, mustExist = false) }
        } catch (_: IOException) {
            return
        }
    }

    fun newFile(): Path {
        fileSystem.createDirectories(root)
        return root / "copy-${Random.nextLong().toULong().toString(radix = 16)}"
    }

    fun delete(path: Path) {
        try {
            fileSystem.delete(path, mustExist = false)
        } catch (_: IOException) {
            return
        }
    }

    companion object {
        val DEFAULT_ROOT: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "caro-import"
    }
}

private fun newSessionName(): String = "$SESSION_PREFIX${Random.nextLong().toULong().toString(radix = 16)}"
