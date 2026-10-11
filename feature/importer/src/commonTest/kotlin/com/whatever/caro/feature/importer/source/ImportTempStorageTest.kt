package com.whatever.caro.feature.importer.source

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import okio.FileSystem
import okio.Path
import kotlin.random.Random

class ImportTempStorageTest : FunSpec() {
    private val fileSystem = FileSystem.SYSTEM

    init {
        test("clearAll은 남은 파일과 하위 디렉터리를 모두 지운다") {
            withSandbox { sandbox ->
                val root = sandbox / "caro-import"
                fileSystem.createDirectories(root / "importparser-1a2b")
                fileSystem.write(root / "importparser-1a2b" / "collection.anki21b") { writeUtf8("db") }
                fileSystem.write(root / "copy-1") { writeUtf8("zip") }

                ImportTempStorage(fileSystem, root).clearAll()

                fileSystem.listOrNull(root).orEmpty() shouldBe emptyList()
            }
        }

        test("clearAll은 caro-import 밖의 파일과 이름이 비슷한 형제 디렉터리를 건드리지 않는다") {
            withSandbox { sandbox ->
                val root = sandbox / "caro-import"
                fileSystem.createDirectories(root)
                fileSystem.write(root / "copy-1") { writeUtf8("zip") }
                fileSystem.write(sandbox / "outside.txt") { writeUtf8("keep") }
                fileSystem.createDirectories(sandbox / "caro-import-other")
                fileSystem.write(sandbox / "caro-import-other" / "keep.txt") { writeUtf8("keep") }

                ImportTempStorage(fileSystem, root).clearAll()

                fileSystem.exists(sandbox / "outside.txt") shouldBe true
                fileSystem.exists(sandbox / "caro-import-other" / "keep.txt") shouldBe true
            }
        }

        test("clearAll은 디렉터리가 아직 없어도 던지지 않는다") {
            withSandbox { sandbox ->
                val storage = ImportTempStorage(fileSystem, sandbox / "caro-import")

                shouldNotThrowAny { storage.clearAll() }
            }
        }

        test("newFile이 돌려준 경로는 caro-import 바로 아래다") {
            withSandbox { sandbox ->
                val root = sandbox / "caro-import"

                val path = ImportTempStorage(fileSystem, root).newFile()

                path.parent shouldBe root
            }
        }

        test("newFile은 디렉터리가 없어도 만들어서 바로 쓸 수 있는 경로를 돌려준다") {
            withSandbox { sandbox ->
                val path = ImportTempStorage(fileSystem, sandbox / "caro-import").newFile()

                fileSystem.write(path) { writeUtf8("zip") }

                fileSystem.read(path) { readUtf8() } shouldBe "zip"
            }
        }

        test("newFile은 호출마다 다른 경로를 돌려준다") {
            withSandbox { sandbox ->
                val storage = ImportTempStorage(fileSystem, sandbox / "caro-import")

                val paths = List(5) { storage.newFile() }

                paths.toSet().size shouldBe 5
            }
        }

        test("delete는 그 파일만 지운다") {
            withSandbox { sandbox ->
                val storage = ImportTempStorage(fileSystem, sandbox / "caro-import")
                val target = storage.newFile().also { path -> fileSystem.write(path) { writeUtf8("a") } }
                val other = storage.newFile().also { path -> fileSystem.write(path) { writeUtf8("b") } }

                storage.delete(target)

                fileSystem.exists(target) shouldBe false
                fileSystem.exists(other) shouldBe true
            }
        }

        test("delete는 이미 없는 파일이어도 던지지 않는다") {
            withSandbox { sandbox ->
                val storage = ImportTempStorage(fileSystem, sandbox / "caro-import")

                shouldNotThrowAny { storage.delete(storage.newFile()) }
            }
        }

        test("기본 위치는 시스템 임시 디렉터리의 caro-import다") {
            val storage = ImportTempStorage()
            val path = storage.newFile()
            try {
                path.parent shouldBe FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "caro-import"
            } finally {
                storage.delete(path)
            }
        }
    }

    private fun withSandbox(block: (Path) -> Unit) {
        val sandbox =
            FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "caro-importer-temp-storage-test" / Random.nextLong().toULong().toString(radix = 16)
        fileSystem.createDirectories(sandbox)
        try {
            block(sandbox)
        } finally {
            fileSystem.deleteRecursively(sandbox, mustExist = false)
        }
    }
}
