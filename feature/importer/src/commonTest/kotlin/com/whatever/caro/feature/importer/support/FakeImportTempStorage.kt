package com.whatever.caro.feature.importer.support

import com.whatever.caro.feature.importer.source.ImportFileCopy
import com.whatever.caro.feature.importer.source.ImportTempStorage
import okio.FileSystem
import okio.Path
import kotlin.random.Random

/**
 * ImportTempStorage가 final 클래스라 대역 대신 테스트마다 따로 만든 샌드박스 디렉터리를 가리키게 한다.
 * 실제 caro-import를 건드리지 않고 지운 파일과 남은 파일을 눈으로 확인할 수 있다.
 */
internal class FakeImportTempStorage private constructor(
    private val fileSystem: FileSystem,
    private val sandbox: Path,
) {
    val root: Path = sandbox / "caro-import"
    val storage: ImportTempStorage = ImportTempStorage(fileSystem, root)
    val fileCopy: ImportFileCopy = ImportFileCopy(fileSystem)

    fun regularFiles(): List<Path> =
        if (fileSystem.exists(root)) {
            fileSystem.listRecursively(root).filter { path -> fileSystem.metadata(path).isRegularFile }.toList()
        } else {
            emptyList()
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
