package com.whatever.caro.core.importparser.archive

import okio.FileSystem
import okio.Path
import kotlin.random.Random

/** 파서가 만드는 임시 파일을 한 디렉터리에 모아 끝나면 디렉터리째 지운다. 원본 사본은 호출자 몫이라 건드리지 않는다. */
internal inline fun <T> withTempWorkspace(
    fileSystem: FileSystem,
    parent: Path,
    block: (Path) -> T,
): T {
    val directory = parent / "importparser-${Random.nextLong().toULong().toString(radix = 16)}"
    fileSystem.createDirectories(directory)
    try {
        return block(directory)
    } finally {
        fileSystem.deleteRecursively(directory, mustExist = false)
    }
}
