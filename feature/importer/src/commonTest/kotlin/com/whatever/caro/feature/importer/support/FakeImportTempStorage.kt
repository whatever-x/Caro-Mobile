package com.whatever.caro.feature.importer.support

import com.whatever.caro.feature.importer.source.ImportFileCopy
import com.whatever.caro.feature.importer.source.ImportTempStorage
import okio.FileSystem
import okio.Path
import kotlin.random.Random

/**
 * ImportTempStorage가 final 클래스라 대역 대신 테스트마다 따로 만든 샌드박스 디렉터리를 가리키게 한다.
 * 실제 caro-import를 건드리지 않고 지운 파일과 남은 파일을 눈으로 확인할 수 있다.
 * 오래됨 기준을 0으로 두어, 다른 화면의 디렉터리는 만들어 둔 즉시 오래된 것으로 본다. 실제 시스템 선택기 디렉터리는 건드리지 않게 비운다.
 */
internal class FakeImportTempStorage private constructor(
    private val fileSystem: FileSystem,
    private val sandbox: Path,
) {
    private val base: Path = sandbox / "caro-import"
    val root: Path = base / "session-own"
    val storage: ImportTempStorage = ImportTempStorage(fileSystem, root, staleAfterMillis = 0L, inboxDirectory = null)
    val fileCopy: ImportFileCopy = ImportFileCopy(fileSystem)

    fun regularFiles(): List<Path> =
        if (fileSystem.exists(root)) {
            fileSystem.listRecursively(root).filter { path -> fileSystem.metadata(path).isRegularFile }.toList()
        } else {
            emptyList()
        }

    fun exists(path: Path): Boolean = fileSystem.exists(path)

    /** 이전 실행이 정리하지 못하고 남긴 다른 화면의 디렉터리. 안에 든 파일의 경로를 돌려준다. */
    fun plantPreviousSession(name: String = "session-previous"): Path {
        fileSystem.createDirectories(base / name)
        return (base / name / "copy-old").also { path -> fileSystem.write(path) { writeUtf8("stale") } }
    }

    fun plantLeftover(name: String = "stale-copy"): Path {
        fileSystem.createDirectories(root)
        return (root / name).also { path -> fileSystem.write(path) { writeUtf8("stale") } }
    }

    fun dispose() {
        fileSystem.deleteRecursively(sandbox, mustExist = false)
    }

    companion object {
        fun create(fileSystem: FileSystem = FileSystem.SYSTEM): FakeImportTempStorage {
            val sandbox =
                FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "caro-importer-viewmodel-test" / Random.nextLong().toULong().toString(radix = 16)
            fileSystem.createDirectories(sandbox)
            return FakeImportTempStorage(fileSystem, sandbox)
        }
    }
}
