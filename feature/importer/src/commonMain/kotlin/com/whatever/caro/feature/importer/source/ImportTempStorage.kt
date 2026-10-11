package com.whatever.caro.feature.importer.source

import okio.FileSystem
import okio.IOException
import okio.Path
import kotlin.random.Random

/** 선택한 원본 사본과 파서의 작업 디렉터리를 [root] 한 곳에 모아 지울 범위를 이 디렉터리로 한정한다. */
internal class ImportTempStorage(
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val root: Path = DEFAULT_ROOT,
) {
    /** 정리 실패 때문에 가져오기를 막지 않는다. 남은 파일은 다음 정리에서 지운다. */
    fun clearAll() {
        try {
            fileSystem.deleteRecursively(root, mustExist = false)
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
