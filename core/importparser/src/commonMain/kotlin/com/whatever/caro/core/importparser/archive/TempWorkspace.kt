package com.whatever.caro.core.importparser.archive

import okio.FileSystem
import okio.IOException
import okio.Path
import kotlin.random.Random

internal const val WORKSPACE_PREFIX = "importparser-"

// 서버는 파싱을 동시에 돌릴 수 있으므로 진행 중인 다른 작업 디렉터리를 지우지 않도록 충분히 오래된 것만 지운다.
internal const val STALE_WORKSPACE_MILLIS = 60L * 60 * 1000

/**
 * 파서가 만드는 임시 파일을 한 디렉터리에 모아 끝나면 디렉터리째 지운다. 원본 사본은 호출자 몫이라 건드리지 않는다.
 * 앱이 파싱 중에 종료돼 남은 이전 디렉터리는 다음 파싱을 시작할 때 지운다.
 *
 * @param nowMillis 오래됨을 판정할 현재 시각. null이면 새로 만든 디렉터리의 수정 시각을 써서 파일 시각과 같은 시계로 비교한다.
 */
internal inline fun <T> withTempWorkspace(
    fileSystem: FileSystem,
    parent: Path,
    nowMillis: Long? = null,
    block: (Path) -> T,
): T {
    val directory = parent / "$WORKSPACE_PREFIX${Random.nextLong().toULong().toString(radix = 16)}"
    fileSystem.createDirectories(directory)
    deleteStaleWorkspaces(fileSystem, parent, directory, nowMillis)
    // 블록 안에서 바깥 함수로 바로 돌아가도 지워지도록 finally에 둔다. 정리 오류는 원래 실패 원인을 가리지 않도록 삼킨다.
    try {
        return block(directory)
    } finally {
        deleteQuietly(fileSystem, directory)
    }
}

internal fun deleteStaleWorkspaces(
    fileSystem: FileSystem,
    parent: Path,
    current: Path,
    nowMillis: Long?,
) {
    val now = nowMillis ?: fileSystem.metadataOrNull(current)?.lastModifiedAtMillis ?: return
    val children = fileSystem.listOrNull(parent) ?: return
    children
        .filter { child -> child != current && child.name.startsWith(WORKSPACE_PREFIX) }
        .filter { child -> fileSystem.metadataOrNull(child)?.lastModifiedAtMillis?.let { now - it >= STALE_WORKSPACE_MILLIS } == true }
        .forEach { child -> deleteQuietly(fileSystem, child) }
}

/** 남은 임시 파일은 다음 파싱이 지우므로 정리 실패 때문에 파싱 결과를 버리지 않는다. */
internal fun deleteQuietly(
    fileSystem: FileSystem,
    directory: Path,
) {
    try {
        fileSystem.deleteRecursively(directory, mustExist = false)
    } catch (_: IOException) {
        return
    }
}
